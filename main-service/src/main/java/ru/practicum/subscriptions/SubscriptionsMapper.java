package ru.practicum.subscriptions;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.practicum.subscriptions.dto.SubscriptionsDto;
import ru.practicum.subscriptions.model.Subscriptions;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SubscriptionsMapper {

    public static SubscriptionsDto mapToSubscriptionDto(Subscriptions subscription) {
        return new SubscriptionsDto(subscription.getSubscriptionId(),
                subscription.getUser(), subscription.getFollower());
    }
}