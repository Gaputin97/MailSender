package by.iba.bussines.meeting.wrapper.definer;

import by.iba.bussiness.meeting.type.MeetingType;
import by.iba.bussiness.meeting.timeslot.model.TimeSlot;

import java.util.ArrayList;
import java.util.List;

public class Data {

    private static final String START_DATE_FOR_SINGLE_EVENT = "12/01/2018 11:30:00";
    private static final String END_DATE_FOR_SINGLE_EVENT = "32/01/2018 11:30:00";
    public static final MeetingType MEETING_TYPE_FOR_SINGLE_EVENT = MeetingType.SINGLE;

    public static List<TimeSlot> createTimeSlotsForSingleEvent() {
        List<TimeSlot> timeSlots = new ArrayList<>();
        TimeSlot timeSlot = new TimeSlot();
        timeSlot.setStartDateTime(START_DATE_FOR_SINGLE_EVENT);
        timeSlot.setEndDateTime(END_DATE_FOR_SINGLE_EVENT);
        timeSlots.add(timeSlot);
        return timeSlots;
    }
}
