package com.togethertrip.notification.notification.infrastructure.sqs

import com.togethertrip.notification.notification.service.NotificationMessageQueue
import com.togethertrip.notification.notification.service.ReceivedNotificationMessage
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest

class SqsNotificationMessageQueue(
    private val sqsClient: SqsClient,
    private val properties: SqsNotificationConsumerProperties,
) : NotificationMessageQueue {

    override fun receive(): List<ReceivedNotificationMessage> =
        sqsClient.receiveMessage(
            ReceiveMessageRequest.builder()
                .queueUrl(properties.queueUrl)
                .maxNumberOfMessages(properties.maxMessages.coerceIn(1, 10))
                .waitTimeSeconds(properties.waitTime.toSeconds().toInt().coerceIn(0, 20))
                .visibilityTimeout(properties.visibilityTimeout.toSeconds().toInt().coerceAtLeast(0))
                .build(),
        ).messages().map { message ->
            ReceivedNotificationMessage(
                id = message.messageId(),
                body = message.body(),
                receiptHandle = message.receiptHandle(),
            )
        }

    override fun acknowledge(message: ReceivedNotificationMessage) {
        sqsClient.deleteMessage(
            DeleteMessageRequest.builder()
                .queueUrl(properties.queueUrl)
                .receiptHandle(message.receiptHandle)
                .build(),
        )
    }
}
