package by.iba.bussines.meeting.wrapper.model;

import by.iba.bussines.meeting.type.MeetingType;

public class MeetingWrapper {
    protected final MeetingType meetingType;

   public MeetingWrapper(MeetingType meetingType) {
        this.meetingType = meetingType;
    }

    public MeetingType getMeetingType() {
        return meetingType;
    }
}
