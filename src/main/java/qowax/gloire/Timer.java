package qowax.gloire;

import java.util.Calendar;
import java.util.Date;

public class Timer {

    // Retourne le temps avant le reset
    public void Timer() {



        // Date aujourd'hui
        Date today = Calendar.getInstance().getTime();

        // Jour remise à zéro
        String resetStr = Gloire.plugin.getConfig().getString("timer.reset_time");


    }

}
