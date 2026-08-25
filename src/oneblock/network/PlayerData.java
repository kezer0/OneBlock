package oneblock.network;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class PlayerData {
    private final UUID uuid;
    private volatile String username;
    private volatile BigDecimal balance;
    private final ConcurrentMap<String, Integer> skills = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, QuestData> quests = new ConcurrentHashMap<>();

    public PlayerData(UUID uuid, String username, BigDecimal balance) {
        this.uuid = uuid;
        this.username = username == null ? "Unknown" : username;
        this.balance = balance == null ? BigDecimal.ZERO.setScale(2) : balance;
    }

    public UUID getUuid() { return uuid; }
    public String getUsername() { return username; }
    public void setUsername(String username) { if (username != null) this.username = username; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance == null ? BigDecimal.ZERO.setScale(2) : balance; }
    public ConcurrentMap<String, Integer> getSkills() { return skills; }
    public ConcurrentMap<String, QuestData> getQuests() { return quests; }

    public static final class QuestData {
        private final String questId;
        private volatile int progress;
        private volatile boolean completed;

        public QuestData(String questId, int progress, boolean completed) {
            this.questId = questId;
            this.progress = progress;
            this.completed = completed;
        }

        public String getQuestId() { return questId; }
        public int getProgress() { return progress; }
        public void setProgress(int progress) { this.progress = progress; }
        public boolean isCompleted() { return completed; }
        public void setCompleted(boolean completed) { this.completed = completed; }
    }
}
