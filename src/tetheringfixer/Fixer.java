package tetheringfixer;

// su -c "iptables -tnat -A natctrl_nat_POSTROUTING -s 192.168.0.0/16 -o rmnet0 -j MASQUERADE"
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.exceptions.RootDeniedException;
import com.stericson.RootTools.execution.Command;
import com.stericson.RootTools.execution.CommandCapture;
import com.stericson.RootTools.execution.Shell;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Fixer {

    private final static String TETHERING_COMMAND = "iptables -tnat -A natctrl_nat_POSTROUTING -s 192.168.0.0/16 -o rmnet0 -j MASQUERADE";
    private final static String ROOT_GAINED_KEY = "pref_root_gained";
    private final static String FIX_AT_BOOT_KEY = "pref_start_at_boot";

    private final static boolean ROOT_GAINED_DEFAULT_VALUE = false;
    private final static boolean FIX_AT_BOOT_DEFAULT_VALUE = true;

    private final static int NOT_FOUND_ERROR = 127;

    /**
     * Returns
     * - true if the fix has been applied
     * - false otherwise
     * */
    public static boolean fixTethering(){
        boolean response = false;

        try {
            //creates the superuser command
            CommandCapture commandCapture = new CommandCapture(0,TETHERING_COMMAND);
            //gets the reference to the root shell
            Shell rootShell = RootTools.getShell(true);
            //adds the command to the execution list
            Command command = rootShell.add(commandCapture);
            //waits until complete - TODO test
            while(command.isExecuting());
            //checks the answer
            int exitCode = command.getExitCode();
            Log.d("mobile.exproductions.fixer.debug","***********************************************\n");
            Log.d("mobile.exproductions.fixer.debug","[TETHERING-FIXER] iptables exit code: " + exitCode);
            Log.d("mobile.exproductions.fixer.debug","\n***********************************************");
            if(exitCode != NOT_FOUND_ERROR) response = true;
            //closes the stream
            rootShell.close();
        } catch (IOException e) {
        } catch (TimeoutException e) {
        } catch (RootDeniedException e) {}
        //does nothing in case of error since we set as default that the action has not completed successfully
        return response;
    }

    /**
     * Loads the root prompt
     * */
    public static void gainRoot(Context mContext){

        //Checks the root permission if and only if it has not been already guaranteed
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        boolean rootAccessGiven = sharedPrefs.getBoolean(ROOT_GAINED_KEY,ROOT_GAINED_DEFAULT_VALUE);
        //this check is not really needed, but it helps at the first start - can be always performed, but
        //gain, in that case, is very little
        if(!rootAccessGiven){
            SharedPreferences.Editor sharedPrefsEditor = sharedPrefs.edit();
            sharedPrefsEditor.putBoolean(ROOT_GAINED_KEY, RootTools.isAccessGiven());
            sharedPrefsEditor.commit();
        }
    }

    public static void setStartAtBoot(Context mContext, boolean startAtBoot){

        //saves user's preference of installing automatically the fix at each boot
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor sharedPrefsEditor = sharedPrefs.edit();
        sharedPrefsEditor.putBoolean(FIX_AT_BOOT_KEY, startAtBoot);
        sharedPrefsEditor.commit();

    }

    public static boolean getStartAtBoot(Context mContext){

        //gets the user preference of automatically install the fix at each booth
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        return sharedPrefs.getBoolean(FIX_AT_BOOT_KEY,FIX_AT_BOOT_DEFAULT_VALUE);

    }

}
