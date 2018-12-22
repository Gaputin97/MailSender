package by.iba.bussiness.rrule.interval;

import by.iba.bussiness.rrule.constants.DateConstants;
import by.iba.bussiness.rrule.frequence.model.RruleFreqType;
import org.springframework.stereotype.Component;

@Component
public class IntervalHelper {

    public long defineInterval(RruleFreqType rruleFreqType, long timeBetweenSessions, long minimumInterval) {
        long millisecondsInFreq = rruleFreqType.getMillisecondsInFreq();
        long freqsInOneHundredYears = rruleFreqType.getMillisecondsInOneHundredYear();
        if (!(minimumInterval == DateConstants.VALUE_FOR_DEFAULT_INTERVAL)) {
            long possibleInterval = timeBetweenSessions / millisecondsInFreq;
            if (possibleInterval < minimumInterval) {
                if (minimumInterval % possibleInterval == 0 || minimumInterval == freqsInOneHundredYears) {
                    minimumInterval = possibleInterval;
                } else {
                    minimumInterval = DateConstants.VALUE_FOR_DEFAULT_INTERVAL;
                }
            } else if (!((possibleInterval) % minimumInterval == 0)) {
                minimumInterval = DateConstants.VALUE_FOR_DEFAULT_INTERVAL;
            }
        }
        return minimumInterval;
    }


}