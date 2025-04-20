package dev.rubasace.linkedin.games_tracker.ranking;

import dev.rubasace.linkedin.games_tracker.group.TelegramGroup;
import dev.rubasace.linkedin.games_tracker.session.GameSession;
import dev.rubasace.linkedin.games_tracker.session.GameSessionService;
import dev.rubasace.linkedin.games_tracker.session.GameType;
import dev.rubasace.linkedin.games_tracker.summary.GroupDailyScore;
import dev.rubasace.linkedin.games_tracker.summary.GroupDailyScoreAdapter;
import dev.rubasace.linkedin.games_tracker.user.TelegramUser;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Transactional(readOnly = true)
@Service
public class GroupRankingService {

    private final GameSessionService gameSessionService;
    private final DailyGameScoreCalculator dailyGameScoreCalculator;
    private final DailyScoreService dailyScoreService;
    private final GroupDailyScoreAdapter groupDailyScoreAdapter;
    private final ApplicationEventPublisher applicationEventPublisher;

    GroupRankingService(final GameSessionService gameSessionService,
                        final DailyGameScoreCalculator dailyGameScoreCalculator,
                        final DailyScoreService dailyScoreService,
                        final GroupDailyScoreAdapter groupDailyScoreAdapter,
                        final ApplicationEventPublisher applicationEventPublisher) {
        this.gameSessionService = gameSessionService;
        this.dailyGameScoreCalculator = dailyGameScoreCalculator;
        this.dailyScoreService = dailyScoreService;
        this.groupDailyScoreAdapter = groupDailyScoreAdapter;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    //TODO make sure this gets performed before the day is over too, in case the ranking wasn't created
    @Transactional
    public void createDailyRanking(TelegramGroup telegramGroup) {
        Set<Long> userIds = telegramGroup.getMembers().stream()
                                         .map(TelegramUser::getId)
                                         .collect(Collectors.toSet());

        Map<GameType, List<GameSession>> groupSessions = gameSessionService.getTodaySessions(userIds, telegramGroup.getChatId())
                                                                           .collect(Collectors.groupingBy(GameSession::getGame));


        Map<GameType, List<DailyGameScore>> gameScores = new HashMap<>();
        for (Map.Entry<GameType, List<GameSession>> entry : groupSessions.entrySet()) {
            GameType gameType = entry.getKey();
            List<GameSession> sessions = entry.getValue();
            List<DailyGameScore> dailyGameScores = dailyGameScoreCalculator.calculateScores(sessions, telegramGroup);
            gameScores.put(gameType, dailyScoreService.updateDailyScores(dailyGameScores, telegramGroup.getChatId(), gameType));
        }
        notifyRankingCreation(telegramGroup.getChatId(), gameScores);
    }

    private void notifyRankingCreation(final Long chatId, final Map<GameType, List<DailyGameScore>> gameScores) {
        GroupDailyScore groupDailyScore = groupDailyScoreAdapter.adapt(chatId, gameScores);
        applicationEventPublisher.publishEvent(new GroupDailyScoreCreatedEvent(this, groupDailyScore));
    }

}
