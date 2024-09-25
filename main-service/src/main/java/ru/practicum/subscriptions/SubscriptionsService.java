package ru.practicum.subscriptions;

import ru.practicum.events.dto.EventRespShort;
import ru.practicum.subscriptions.dto.SubscriptionsDto;
import ru.practicum.users.dto.UserDto;

import java.util.List;

public interface SubscriptionsService {

    SubscriptionsDto subscribeToUser(long userId, long followerId);

    void cancelSubscribe(long userId, long followerId);

    List<UserDto> getUsersIFollow(long userId);

    List<UserDto> getMyFollowers(long userId);

    List<EventRespShort> getUsersEvents(long userId, int from, int size);

}