package dev.leonlatsch.olivia.rest.dto;

import dev.leonlatsch.olivia.database.model.MessageType;

/**
 * @author Leon Latsch
 * @since 1.0.0
 */
public class MessageDTO {

    private String mid;
    private String from;
    private String to;
    private MessageType type;
    private String timestamp;
    private String content;
    private String cid;

    public MessageDTO() {
    }

    public MessageDTO(String mid, String from, String to, MessageType type, String timestamp, String content, String cid) {
        this.mid = mid;
        this.from = from;
        this.to = to;
        this.type = type;
        this.timestamp = timestamp;
        this.content = content;
        this.cid = cid;
    }

    public String getMid() {
        return mid;
    }

    public void setMid(String mid) {
        this.mid = mid;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCid() {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }
}
