package tetheringfixer;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

public class FixerActivity extends Activity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener{
    /**
     * Called when the activity is first created.
     */

    Button btnFixNow;
    Switch swFixAtBoot;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        btnFixNow = (Button) findViewById(R.id.btnFix);
        btnFixNow.setOnClickListener(this);

        swFixAtBoot = (Switch) findViewById(R.id.switchStartAtBoot);
        swFixAtBoot.setOnCheckedChangeListener(this);

        Fixer.gainRoot(this);
    }

    @Override
    public void onClick(View view) {

        switch(view.getId()){
            case R.id.btnFix:
                boolean response = Fixer.fixTethering();
                String message = "";
                if(response) message = "Fix set correctly";
                else message = "Something went wrong - check the logs";

                Toast.makeText(this,message,Toast.LENGTH_SHORT).show();
                break;
            default: break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

        switch(compoundButton.getId()){
            case R.id.switchStartAtBoot:
                Fixer.setStartAtBoot(this,b);
                break;
            default: break;
        }
    }
}
