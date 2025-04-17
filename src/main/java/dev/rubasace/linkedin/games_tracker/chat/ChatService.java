package dev.rubasace.linkedin.games_tracker.chat;

import dev.rubasace.linkedin.games_tracker.assets.AssetsDownloader;
import dev.rubasace.linkedin.games_tracker.game.GameDetector;
import dev.rubasace.linkedin.games_tracker.game.GameType;
import dev.rubasace.linkedin.games_tracker.image.ImageHelper;
import dev.rubasace.linkedin.games_tracker.image.ImageTimeExtractor;
import dev.rubasace.linkedin.games_tracker.util.FormatUtils;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.bytedeco.opencv.global.opencv_imgcodecs.imwrite;

@Service
class ChatService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatService.class);
    private static final String RESULTS_COLOR = "#FDE4A5";

    private final GameDetector gameDetector;
    private final AssetsDownloader assetsDownloader;
    private final TelegramClient telegramClient;
    private final ImageHelper imageHelper;
    private final ImageTimeExtractor imageTimeExtractor;

    private ChatService(final GameDetector gameDetector, final AssetsDownloader assetsDownloader, final TelegramClient telegramClient, final ImageHelper imageHelper, final ImageTimeExtractor imageTimeExtractor) {
        this.gameDetector = gameDetector;
        this.assetsDownloader = assetsDownloader;
        this.telegramClient = telegramClient;
        this.imageHelper = imageHelper;
        this.imageTimeExtractor = imageTimeExtractor;
    }

    void processPhoto(final List<PhotoSize> photoSizeList, final String username, final Long chatId) {

        File imageFile = assetsDownloader.getImage(photoSizeList);
        Optional<GameType> gameType = gameDetector.detectGame(imageFile);

        if (gameType.isEmpty()) {
            return;
        }
        try (Mat image = opencv_imgcodecs.imread(imageFile.getAbsolutePath())) {
            Optional<Rect> yellowBox = imageHelper.findLargestRegionOfColor(image, RESULTS_COLOR);
            yellowBox.ifPresent(rect -> {
                try {
                    Mat cropped = new Mat(image, rect);
                    File temp = File.createTempFile("time-results-section", ".png");
                    imwrite(temp.getAbsolutePath(), cropped);
                    Duration duration = imageTimeExtractor.extractTime(temp);
                    sendMessage(
                            "@%s submitted a screenshot for todays %s taking a total time of %s".formatted(username, gameType.get().name(), FormatUtils.formatDuration(duration)),
                            chatId);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });

        }

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
