package dev.rubasace.linkedin.games_tracker.chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Service
class MessageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageService.class);

    private final TelegramClient telegramClient;

    MessageService(final TelegramClient telegramClient) {
        this.telegramClient = telegramClient;
    }

    void info(final String text, final Long chatId) {
        sendMessage(text, chatId);
    }

    void success(final String text, final Long chatId) {
        sendMessage("✅ " + text, chatId);
    }

    void error(final String text, final Long chatId) {
        sendMessage("❌ " + text, chatId);
    }

    private void sendMessage(final String text, final Long chatId) {
        SendMessage message = SendMessage.builder()
                                         .chatId(chatId)
                                         .text(text)
                                         .build();
        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            LOGGER.error("Error sending message", e);
        }
    }
}
