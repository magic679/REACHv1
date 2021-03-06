package asu.reach;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.util.Calendar;


public class STIC extends Activity implements View.OnClickListener, DialogInterface.OnClickListener{
    //The Android's default system path of your application database.
    private SQLiteDatabase db;
    private LinearLayout list;
    private Button selected;
    private EditText pin;
    private int currentDay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_stic);

        list = (LinearLayout)findViewById(R.id.sticList);

        try{
            DBHelper helper = new DBHelper(this);
            db = helper.getDB();
            currentDay = helper.getCurrentDay();
            Calendar ca = Calendar.getInstance();
            ca.set(ca.get(Calendar.YEAR), ca.get(Calendar.MONTH),ca.get(Calendar.DAY_OF_MONTH),0,0,0);
            Cursor c = db.rawQuery("SELECT ACTIVITY FROM STIC_COMPLETION where TIMESTAMP < "
                    + ca.getTimeInMillis()
                    + " AND ACTIVITY not in (SELECT ACTIVITY FROM "
                    + "STIC_COMPLETION where TIMESTAMP > "
                    + ca.getTimeInMillis() + ")", null);
            c.moveToFirst();
            ContentValues v;
            for(int x = 0; x < c.getCount(); x++){
                System.out.println();
                v = new ContentValues();
                v.put("STIC_COMPLETED_FLAG", 0);
                db.update("STIC",v,"STIC_TASK = \"" + c.getString(c.getColumnIndex("ACTIVITY"))+ "\"",null);
                c.moveToNext();
            }
            c.close();
            c = db.rawQuery("SELECT STIC FROM ADMIN_ACTIVITY_SCHEDULER WHERE DAY = " + currentDay, null);
            c.moveToFirst();
            System.out.println("current day: "+ currentDay + " STIC: " + c.getInt(c.getColumnIndex("STIC")));
            if(c.getInt(c.getColumnIndex("STIC")) == 0){
                Toast.makeText(this, "No S.T.I.C.\nscheduled today.",Toast.LENGTH_LONG).show();
                c.close();
            }else {
                String where = "";
                if(c.getInt(c.getColumnIndex("STIC")) == 1){
                    where = " where QUESTION_SET = 1";
                }
                if(c.getInt(c.getColumnIndex("STIC")) == 2){
                    where = " where QUESTION_SET = 2";
                }
                c.close();
                c = db.rawQuery("SELECT * from STIC" + where, null);
                c.moveToFirst();
                Button btn;
                RelativeLayout.LayoutParams pa;
                RelativeLayout r;
                ImageView i;
                for (int x = 0; x < c.getCount(); x++) {
                    r = new RelativeLayout(this);
                    btn = new Button(this);
                    i = new ImageView(this);
                    btn.setBackgroundResource(R.drawable.stic_btn_selector);
                    btn.setText(c.getString(c.getColumnIndex("STIC_TASK")));
                    btn.setTypeface(Typeface.createFromAsset(getAssets(), "agentorange.ttf"));
                    btn.setPadding(65, 65, 65, 65);
                    btn.setLineSpacing(2, 1.1f);
                    r.setPadding(0, 0, 0, 25);
                    r.addView(btn);
                    pa = new RelativeLayout.LayoutParams(
                            ((int) (100 * 1.312)), 100);
                    pa.addRule(RelativeLayout.CENTER_VERTICAL);
                    pa.setMargins(15, 0, 0, 0);
                    i.setAdjustViewBounds(true);
                    i.setLayoutParams(pa);
                    if (c.getInt(c.getColumnIndex("STIC_COMPLETED_FLAG")) != 0) {
                        i.setBackgroundResource(R.drawable.thumbs_up);
                        btn.setActivated(true);
                    }
                    r.addView(i);
                    btn.setOnClickListener(this);
                    list.addView(r);
                    c.moveToNext();
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_stic, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        if(v.getClass() == Button.class){
            Button btn = (Button)v;
            if(!btn.isActivated()) {
                selected = btn;
                pin = new EditText(this);
                try {
                    DBHelper helper = new DBHelper(this);
                    helper.trackEvent(helper,"STIC_TEACHER_PIN","INSIDE_STIC_ACTIVITY");
                }catch(Exception e){
                    e.printStackTrace();
                }
                pin.setHint("Please Enter A PIN");
                FragmentManager fm = getFragmentManager();
                DialogBuilder dialog = DialogBuilder.newInstance("Confirm", this, pin);
                dialog.show(fm, "frag");
            }
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which){
            case DialogInterface.BUTTON_POSITIVE:{
                try {
                    Cursor c = db.rawQuery("SELECT OWNER from PINS where PIN = "
                            + pin.getText().toString(),null);
                    c.moveToFirst();
                    String owner = c.getString(c.getColumnIndex("OWNER"));
                    ContentValues v = new ContentValues();
                    v.put("TIMESTAMP", System.currentTimeMillis());
                    v.put("ACTIVITY", selected.getText().toString());
                    v.put("OWNER", owner);
                    db.insert("STIC_COMPLETION", "TIMESTAMP,ACTIVITY,OWNER", v);
                    v = new ContentValues();
                    v.put("STIC_COMPLETED_FLAG", 1);
                    db.update("STIC", v, "STIC_TASK = '"
                            + selected.getText().toString() + "'", null);
                    selected.setActivated(true);
                    RelativeLayout l = (RelativeLayout) selected.getParent();
                    ImageView i = (ImageView) l.getChildAt(1);
                    i.setBackgroundResource(R.drawable.thumbs_up);
                    try {
                        DBHelper helper = new DBHelper(this);
                        helper.trackEvent(helper,"STIC_COMPLETED","INSIDE_STIC_ACTIVITY");
                        db = helper.getDB();
                        currentDay = helper.getCurrentDay();
                        if (currentDay < 43 && currentDay > 0) {
                            v = new ContentValues();
                            v.put("STIC", 1);
                            db.update("USER_ACTIVITY_TRACK", v, "DAY = " + currentDay, null);
                        } else {
                            Toast.makeText(this, "Invalid day,\nplease change\nstart date",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                }
                catch(Exception e){
                    Toast.makeText(this, "Invalid PIN", Toast.LENGTH_SHORT).show();
                    try {
                        DBHelper helper = new DBHelper(this);
                        helper.trackEvent(helper,"INVALID_PIN","INSIDE_STIC_ACTIVITY");
                    }catch(Exception e1){
                        e.printStackTrace();
                    }
                    pin = new EditText(this);
                    FragmentManager fm = getFragmentManager();
                    DialogBuilder d = DialogBuilder.newInstance("Confirm", this, pin);
                    d.show(fm, "frag");
                    e.printStackTrace();
                }
                break;
            }
            case DialogInterface.BUTTON_NEGATIVE:{

                break;
            }

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        db.close();
    }

    @Override
    public void onBackPressed() {}

    public void onBackClick(View v){
        finish();
    }
}
