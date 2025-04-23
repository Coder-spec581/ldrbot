package dev.rubasace.linkedin.games.ldrbot;

import dev.rubasace.linkedin.games.ldrbot.group.GroupNotFoundException;
import dev.rubasace.linkedin.games.ldrbot.group.TelegramGroup;
import dev.rubasace.linkedin.games.ldrbot.group.TelegramGroupRepository;
import dev.rubasace.linkedin.games.ldrbot.group.TelegramGroupService;
import dev.rubasace.linkedin.games.ldrbot.ranking.DailyGameScore;
import dev.rubasace.linkedin.games.ldrbot.ranking.DailyScoreRepository;
import dev.rubasace.linkedin.games.ldrbot.session.AlreadyRegisteredSession;
import dev.rubasace.linkedin.games.ldrbot.session.GameDuration;
import dev.rubasace.linkedin.games.ldrbot.session.GameSession;
import dev.rubasace.linkedin.games.ldrbot.session.GameSessionRepository;
import dev.rubasace.linkedin.games.ldrbot.session.GameSessionService;
import dev.rubasace.linkedin.games.ldrbot.session.GameType;
import dev.rubasace.linkedin.games.ldrbot.user.TelegramUser;
import dev.rubasace.linkedin.games.ldrbot.user.TelegramUserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
public class TestController {

    private final TelegramGroupRepository telegramGroupRepository;
    private final TelegramGroupService telegramGroupService;
    private final GameSessionService gameSessionService;
    private final GameSessionRepository gameSessionRepository;
    private final TelegramUserRepository telegramUserRepository;
    private final DailyScoreRepository dailyScoreRepository;

    public TestController(final TelegramGroupRepository telegramGroupRepository, final TelegramGroupService telegramGroupService, final GameSessionService gameSessionService, final GameSessionRepository gameSessionRepository, final TelegramUserRepository telegramUserRepository, final DailyScoreRepository dailyScoreRepository) {
        this.telegramGroupRepository = telegramGroupRepository;
        this.telegramGroupService = telegramGroupService;
        this.gameSessionService = gameSessionService;
        this.gameSessionRepository = gameSessionRepository;
        this.telegramUserRepository = telegramUserRepository;
        this.dailyScoreRepository = dailyScoreRepository;
    }

    @GetMapping("groups")
    Iterable<TelegramGroup> getGroups() {
        return telegramGroupRepository.findAll();
    }


    @GetMapping("sessions")
    public Iterable<GameSession> getSessions() {
        return gameSessionRepository.findAll();
    }

    @GetMapping("users")
    public Iterable<TelegramUser> getUsers() {
        return telegramUserRepository.findAll();
    }

    @GetMapping("scores")
    public Iterable<DailyGameScore> getScores() {
        return dailyScoreRepository.findAll();
    }

    @GetMapping("fake")
    public void fakeTimes(@RequestParam Long chatId) throws GroupNotFoundException, AlreadyRegisteredSession {
        long aliceId = 231897429L;
        long benId = 231235214L;
        long johnId = 928447421L;
        long georgeId = 831743129L;
        telegramGroupService.addUserToGroup(chatId, aliceId, "alice");
        telegramGroupService.addUserToGroup(chatId, benId, "ben");
        telegramGroupService.addUserToGroup(chatId, johnId, "john");
        telegramGroupService.addUserToGroup(chatId, georgeId, "george");

        gameSessionService.recordGameSession(aliceId, chatId, "alice", new GameDuration(GameType.ZIP, Duration.ofSeconds(20)));
        gameSessionService.recordGameSession(aliceId, chatId, "alice", new GameDuration(GameType.QUEENS, Duration.ofSeconds(33)));
        gameSessionService.recordGameSession(aliceId, chatId, "alice", new GameDuration(GameType.TANGO, Duration.ofSeconds(27)));

        gameSessionService.recordGameSession(benId, chatId, "ben", new GameDuration(GameType.ZIP, Duration.ofSeconds(21)));
        gameSessionService.recordGameSession(benId, chatId, "ben", new GameDuration(GameType.QUEENS, Duration.ofSeconds(32)));
        gameSessionService.recordGameSession(benId, chatId, "ben", new GameDuration(GameType.TANGO, Duration.ofSeconds(27)));

        gameSessionService.recordGameSession(johnId, chatId, "john", new GameDuration(GameType.ZIP, Duration.ofSeconds(21)));
        gameSessionService.recordGameSession(johnId, chatId, "john", new GameDuration(GameType.QUEENS, Duration.ofSeconds(33)));
        gameSessionService.recordGameSession(johnId, chatId, "john", new GameDuration(GameType.TANGO, Duration.ofSeconds(19)));

        gameSessionService.recordGameSession(georgeId, chatId, "george", new GameDuration(GameType.ZIP, Duration.ofSeconds(21)));
        gameSessionService.recordGameSession(georgeId, chatId, "george", new GameDuration(GameType.QUEENS, Duration.ofSeconds(40)));
        gameSessionService.recordGameSession(georgeId, chatId, "george", new GameDuration(GameType.TANGO, Duration.ofSeconds(29)));


    }

}
