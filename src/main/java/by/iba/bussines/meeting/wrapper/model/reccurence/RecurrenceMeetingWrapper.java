package by.iba.bussines.meeting.wrapper.model.reccurence;

import by.iba.bussines.calendar.factory.type.MeetingType;
import by.iba.bussines.meeting.wrapper.model.MeetingWrapper;
import by.iba.bussines.rrule.model.Rrule;

public class RecurrenceMeetingWrapper extends MeetingWrapper {
    private Rrule rrule;

    public RecurrenceMeetingWrapper() {
        super(MeetingType.RECURRENCE);
    }

    public Rrule getRrule() {
        return rrule;
    }

    public void setRrule(Rrule rrule) {
        this.rrule = rrule;
    }
}
