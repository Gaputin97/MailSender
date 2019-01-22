package by.iba.bussiness.template;

import by.iba.bussiness.owner.Owner;

public class Template {

    private String summary;
    private String description;
    private String location;
    private String sessions;
    private String type;
    private String from;
    private String fromName;

    public Template() {
    }

    public Template(Template template) {
        this.summary = template.getSummary();
        this.from = template.getFrom();
        this.fromName = template.getFromName();
        this.description = template.getDescription();
        this.sessions = template.getSessions();
    }


    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getFromName() {
        return fromName;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getSessions() {
        return sessions;
    }

    public void setSessions(String timeSlots) {
        this.sessions = timeSlots;
    }
}
