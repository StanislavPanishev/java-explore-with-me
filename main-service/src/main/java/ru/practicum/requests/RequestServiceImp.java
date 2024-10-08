package ru.practicum.requests;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.errors.ConflictException;
import ru.practicum.errors.NotFoundException;
import ru.practicum.events.EventRepository;
import ru.practicum.events.EventStates;
import ru.practicum.events.model.Event;
import ru.practicum.requests.dto.RequestDto;
import ru.practicum.requests.model.Requests;
import ru.practicum.users.UserRepository;
import ru.practicum.users.model.User;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RequestServiceImp implements RequestService {

    private final RequestRepository requestRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    @Override
    public RequestDto addRequest(long eventId, long userId) {
        Event event = validateEvent(eventId); //Check event
        User user = validateUser(userId);
        checkAbilityToAddRequest(event, userId, eventId);
        Requests addingRequest = Requests
                .builder()
                .created(LocalDateTime.now())
                .build();

        addingRequest.setEvent(event);
        addingRequest.setRequester(user); //Check and set user

        addingRequest.setStatus(String.valueOf(RequestStatus.PENDING));

        if (!event.getRequestModeration()) {
            addingRequest.setStatus(String.valueOf(RequestStatus.CONFIRMED));
        }

        if (event.getParticipantLimit() == 0) {
            addingRequest.setStatus(String.valueOf(RequestStatus.CONFIRMED));
        }

        return RequestMapper.mapToRequestDto(requestRepository.save(addingRequest));
    }

    @Override
    public RequestDto cancelRequest(long requestId, long userId) {
        Requests updatingRequest = validateRequest(requestId);
        updatingRequest.setStatus(String.valueOf(RequestStatus.CANCELED));
        return RequestMapper.mapToRequestDto(requestRepository.save(updatingRequest));
    }

    @Override
    public Collection<RequestDto> getMyRequests(long userId) {
        return requestRepository
                .findByRequesterId(userId)
                .stream()
                .map(RequestMapper::mapToRequestDto)
                .collect(Collectors.toSet());
    }

    private void checkAbilityToAddRequest(Event event, long requesterId, long eventId) {
        List<Requests> requests = requestRepository.findByRequesterId(requesterId);
        isThereDuplicate(requests, requesterId, eventId);
        validateEventStatesAndRequesterId(event, requesterId);
        checkParticipantsLimit(event);
    }

    private void validateEventStatesAndRequesterId(Event event, Long requesterId) {
        if (event.getState().equals(String.valueOf(EventStates.PENDING))
                || event.getState().equals(String.valueOf(EventStates.CANCELED))) {
            log.warn("Event with EventId: {} is not published. Request is canceled", event.getId());
            throw new ConflictException("Event with id = " + event.getId() + " is not published");
        }

        if (event.getInitiator().getId().equals(requesterId)) {
            log.warn("Attempt to add request to self event");
            throw new ConflictException("Could not add request to event");
        }
    }

    private void checkParticipantsLimit(Event event) {
        long requestAmountForEvent = requestRepository.countByEventIdAndStatus(event.getId(),
                String.valueOf(RequestStatus.CONFIRMED));

        if ((event.getParticipantLimit() != 0)
                && (event.getParticipantLimit() < (requestAmountForEvent + 1))) {
            log.warn("Unable to add request. ParticipantLimit {} less then request amount {}",
                    event.getParticipantLimit(), (requestAmountForEvent + 1));
            throw new ConflictException("Exceeded max requesters amount");
        }
    }

    private Requests validateRequest(long requestId) {
        Optional<Requests> request = requestRepository.findById(requestId);

        if (request.isEmpty()) {
            log.warn("Attempt to get unknown event");
            throw new NotFoundException("Request with id = " + request + " was not found");
        }
        return request.get();
    }

    private Event validateEvent(long eventId) {
        Optional<Event> event = eventRepository.findById(eventId);

        if (event.isEmpty()) {
            log.warn("Attempt to get unknown event");
            throw new NotFoundException("Event with id = " + eventId + " was not found");
        }
        return event.get();
    }

    private User validateUser(long userId) {
        Optional<User> user = userRepository.findById(userId);

        if (user.isEmpty()) {
            log.warn("Attempt to delete unknown user");
            throw new NotFoundException("User with id = " + userId + " was not found");
        }
        return user.get();
    }

    private static void sortByRequesterIdAndEventId(List<Requests> list) {
        list.sort((Requests req1, Requests req2) -> {
            if (req1.getRequester().getId() > req2.getRequester().getId()) {
                return 1;
            } else if (req1.getRequester().getId().equals(req2.getRequester().getId())) {
                return req1.getEvent().getId().compareTo(req2.getEvent().getId());
            } else {
                return -1;
            }
        });
    }

    private static void isThereDuplicate(List<Requests> list, long requesterTargetId, long eventTargetId) {
        sortByRequesterIdAndEventId(list);

        int low = 0;
        int high = list.size() - 1;
        int mid;

        while (low <= high) {
            mid = (low + high) / 2;

            if (list.get(mid).getRequester().getId().equals(requesterTargetId)) {
                if (list.get(mid).getEvent().getId().equals(eventTargetId)) {
                    log.warn("Request with id: {} is duplicate", requesterTargetId);
                    throw new ConflictException("Request with id = " + requesterTargetId + " is duplicate");
                } else {
                    if (list.get(mid).getEvent().getId() < eventTargetId) {
                        low = mid + 1;
                    } else {
                        high = mid - 1;
                    }
                }

            } else if (list.get(mid).getRequester().getId() < requesterTargetId) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
    }
}

