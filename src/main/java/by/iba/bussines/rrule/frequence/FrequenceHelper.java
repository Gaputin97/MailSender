package by.iba.bussines.rrule.frequence;

import by.iba.bussines.rrule.frequence.model.RruleFreqType;

public class FrequenceHelper {

    public boolean isDurationMultipleToFreq(RruleFreqType rruleFreqType, long timeBetweenSessions) {
        boolean isDurationMultipleToFreq = false;
        long millisecondsInFreq = rruleFreqType.getMillisecondsInFreq();
        if (timeBetweenSessions % millisecondsInFreq == 0) {
            isDurationMultipleToFreq = true;
        }
        return isDurationMultipleToFreq;
    }


}