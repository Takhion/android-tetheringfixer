package tetheringfixer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootBroadcastReceiver extends BroadcastReceiver{
    @Override
    public void onReceive(Context context, Intent intent) {

        //fixes the tethering if and only if user requested to do it at each boot
        if(Fixer.getStartAtBoot(context))
            Fixer.fixTethering();

    }
}
