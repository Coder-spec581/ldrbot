package dev.rubasace.linkedin.games_tracker.chat;

import dev.rubasace.linkedin.games_tracker.configuration.ExecutorsConfiguration;
import dev.rubasace.linkedin.games_tracker.group.GroupCreatedEvent;
import dev.rubasace.linkedin.games_tracker.group.UserJoinedGroupEvent;
import dev.rubasace.linkedin.games_tracker.group.UserLeftGroupEvent;
import dev.rubasace.linkedin.games_tracker.image.GameDurationExtractionException;
import dev.rubasace.linkedin.games_tracker.ranking.GroupDailyScoreCreatedEvent;
import dev.rubasace.linkedin.games_tracker.session.AlreadyRegisteredSession;
import dev.rubasace.linkedin.games_tracker.session.GameNameNotFoundException;
import dev.rubasace.linkedin.games_tracker.session.GameSessionRegistrationEvent;
import dev.rubasace.linkedin.games_tracker.session.GameType;
import dev.rubasace.linkedin.games_tracker.summary.GameScoreData;
import dev.rubasace.linkedin.games_tracker.summary.GlobalScoreData;
import dev.rubasace.linkedin.games_tracker.summary.GroupDailyScore;
import dev.rubasace.linkedin.games_tracker.user.UsernameNotFoundException;
import dev.rubasace.linkedin.games_tracker.util.FormatUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class NotificationService {

    private static final String ALREADY_REGISTERED_SESSION_MESSAGE_TEMPLATE = "@%s already registered a time for %s. If you need to override the time, please delete the current time through the \"/delete <game>\" command. In this case: /delete %s. Alternatively, you can delete all your submissions for the day using /deleteall";
    private static final String SUBMISSION_MESSAGE_TEMPLATE = "@%s submitted their result for today's %s with a time of %s";
    private static final String USER_JOIN_MESSAGE_TEMPLATE = "User @%s joined this group";
    private static final String USER_LEAVE_MESSAGE_TEMPLATE = "User @%s left this group";
    private static final String GROUP_GREETING_MESSAGE = """
            👋 Hey everyone, I'm your LinkedIn Games Tracker bot 🤖!
            
            This group is now officially being tracked 🏁. From now on, you can submit your LinkedIn puzzle screenshots, and I’ll keep score for the day.
            
            Every day is a new competition — submit your time, climb the leaderboard, and don’t get left behind! 💪
            
            Type /help to see everything I can do.
            """;
    public static final int GREETING_NOTIFICATION_ORDER = Ordered.HIGHEST_PRECEDENCE;
    public static final int USER_INTERACTION_NOTIFICATION_ORDER = GREETING_NOTIFICATION_ORDER + 1000;
    public static final int DAILY_RANKING_NOTIFICATION_ORDER = 0;

    private final MessageService messageService;

    NotificationService(final MessageService messageService) {
        this.messageService = messageService;
    }

    public void notifyUserFeedbackException(final UserFeedbackException userFeedbackException) {
        if (userFeedbackException instanceof AlreadyRegisteredSession alreadyRegisteredSession) {
            messageService.error(ALREADY_REGISTERED_SESSION_MESSAGE_TEMPLATE.formatted(alreadyRegisteredSession.getUsername(),
                                                                                       alreadyRegisteredSession.getGame().name(),
                                                                                       alreadyRegisteredSession.getGame().name().toLowerCase()),
                                 alreadyRegisteredSession.getChatId());
        } else if (userFeedbackException instanceof UsernameNotFoundException usernameNotFoundException) {
            messageService.error("User @%s not found".formatted(usernameNotFoundException.getUsername()), usernameNotFoundException.getChatId());
        } else if (userFeedbackException instanceof GameNameNotFoundException gameNameNotFoundException) {
            messageService.error("'%s' is not a valid game.".formatted(gameNameNotFoundException.getGameName()), gameNameNotFoundException.getChatId());
        } else if (userFeedbackException instanceof GameDurationExtractionException gameDurationExtractionException) {
            messageService.error(
                    "@%s submitted a screenshot for the game %s but I wasn't able to extract the time. Please try with another image or ask an admin to input the time manually using the command /override %s <time>".formatted(
                            gameDurationExtractionException.getUserName(), gameDurationExtractionException.getGameType().name(),
                            gameDurationExtractionException.getGameType().name().toLowerCase()),
                    gameDurationExtractionException.getChatId());
        }
    }

    @Order(GREETING_NOTIFICATION_ORDER)
    @Async(ExecutorsConfiguration.NOTIFICATION_LISTENER_EXECUTOR_NAME)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void handleGroupCreation(final GroupCreatedEvent groupCreatedEvent) {
        messageService.info(GROUP_GREETING_MESSAGE, groupCreatedEvent.getChatId());
    }

    @Order(DAILY_RANKING_NOTIFICATION_ORDER)
    @Async(ExecutorsConfiguration.NOTIFICATION_LISTENER_EXECUTOR_NAME)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void notifyDailyRanking(final GroupDailyScoreCreatedEvent groupDailyScoreCreatedEvent) {
        GroupDailyScore groupDailyScore = groupDailyScoreCreatedEvent.getGroupDailyScore();
        if (groupDailyScore.globalScore().isEmpty()) {
            messageService.error("No games have been registered yet, cannot calculate te ranking", groupDailyScore.chatId());
            return;
        }

        String htmlSummary = toHtmlSummary(groupDailyScore);
        messageService.html(htmlSummary, groupDailyScore.chatId());
    }

    @Order(USER_INTERACTION_NOTIFICATION_ORDER)
    @Async(ExecutorsConfiguration.NOTIFICATION_LISTENER_EXECUTOR_NAME)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void handleSessionRegistration(final GameSessionRegistrationEvent gameSessionRegistrationEvent) {
        messageService.info(SUBMISSION_MESSAGE_TEMPLATE.formatted(gameSessionRegistrationEvent.getUserName(),
                                                                  gameSessionRegistrationEvent.getGame().name().toLowerCase(),
                                                                  FormatUtils.formatDuration(gameSessionRegistrationEvent.getDuration())),
                            gameSessionRegistrationEvent.getChatId());
    }

    @Order(USER_INTERACTION_NOTIFICATION_ORDER)
    @Async(ExecutorsConfiguration.NOTIFICATION_LISTENER_EXECUTOR_NAME)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void handleUserJoin(final UserJoinedGroupEvent userJoinedGroupEvent) {
        messageService.info(USER_JOIN_MESSAGE_TEMPLATE.formatted(userJoinedGroupEvent.getUserName()),
                            userJoinedGroupEvent.getChatId());
    }

    @Order(USER_INTERACTION_NOTIFICATION_ORDER)
    @Async(ExecutorsConfiguration.NOTIFICATION_LISTENER_EXECUTOR_NAME)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void handleUserLeave(final UserLeftGroupEvent userLeftGroupEvent) {
        messageService.info(USER_LEAVE_MESSAGE_TEMPLATE.formatted(userLeftGroupEvent.getUserName()),
                            userLeftGroupEvent.getChatId());
    }

    private String toHtmlSummary(GroupDailyScore groupScore) {
        StringBuilder sb = new StringBuilder();
        sb.append("<b>📊 Daily Ranking</b>\n");

        for (Map.Entry<GameType, List<GameScoreData>> entry : groupScore.gameScores().entrySet()) {
            toHtmlGameRanking(entry.getKey(), entry.getValue(), sb);
        }

        List<GlobalScoreData> global = groupScore.globalScore();

        toHtmlGlobalRanking(sb, global);

        toHtmlFinalMessage(groupScore.winners(), sb);

        return sb.toString();
    }

    private void toHtmlGameRanking(final GameType gameType, final List<GameScoreData> scores, final StringBuilder sb) {
        sb.append(toTile(FormatUtils.gameIcon(gameType), gameType.name()));

        for (int i = DAILY_RANKING_NOTIFICATION_ORDER; i < scores.size(); i++) {
            GameScoreData score = scores.get(i);
            sb.append(formatRankingLine(i, score.username(), score.duration(), score.points()));
        }
    }

    private String toTile(final String icon, final String title) {
        return "\n<b><u>%s</u> </b>\n".formatted(title);
    }

    private void toHtmlGlobalRanking(final StringBuilder sb, final List<GlobalScoreData> global) {

        sb.append(toTile("🏆", "Global Score"));

        for (int i = DAILY_RANKING_NOTIFICATION_ORDER; i < global.size(); i++) {
            GlobalScoreData score = global.get(i);
            sb.append(formatRankingLine(i, score.username(), score.totalDuration(), score.points()));
        }
    }

    //TODO allow admin to make message configurable
    private void toHtmlFinalMessage(final List<String> winners, final StringBuilder sb) {
        sb.append("\n<b>🎉🎉🎉 Congratulations @%s, you are today's champion%s! 🎉🎉🎉</b>"
                          .formatted(String.join(" and @", winners), winners.size() > 1 ? "s" : ""));
    }

    private String formatRankingLine(int position, String username, Duration duration, int points) {
        String icon = rankingIcon(position);
        String paddedUser = String.format("@%s", username);
        String durationStr = FormatUtils.formatDuration(duration);
        return String.format("%s %s (%s) — %d pts\n", icon, paddedUser, durationStr, points);
    }

    private String rankingIcon(int position) {
        return switch (position) {
            case DAILY_RANKING_NOTIFICATION_ORDER -> "🥇";
            case 1 -> "🥈";
            case 2 -> "🥉";
            case 3 -> "4️⃣";
            case 4 -> "5️⃣";
            case 5 -> "6️⃣";
            case 6 -> "7️⃣";
            case 7 -> "8️⃣";
            case 8 -> "9️⃣";
            case 9 -> "🔟";
            default -> (position + 1) + ".";
        };
    }


}
