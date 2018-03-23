package com.onionapps.alamoconverter;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Paint;
import android.location.Location;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.Time;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import java.time.Instant;
import java.util.GregorianCalendar;

import static java.lang.Math.PI;
import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.pow;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;

public class MainActivity extends AppCompatActivity {
    int kg,s,m;

    final double[] alamoLoc = { 29.4256989,-98.4861322,36};//from the centroid on google maps
    // Note this is for the centroid on google maps

    final double alamoMass = 132065600;//kg: 55960 m^3 volume and 2360 kg/m^3 density

    final long alamoTime = (new GregorianCalendar(1836,3,6,5,30)).getTimeInMillis();//5:30 a.m. start time of battle on March 6, 1836

    LocationManager lm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

        View topView = getLayoutInflater().inflate(R.layout.activity_main,null,false);
        Button refresh = (Button) topView.findViewById(R.id.refreshButton);
        refresh.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                updateConversion();
            }
        });

        //copied from android guide on spinners
        Spinner spinner = (Spinner) findViewById(R.id.unitsSpinner);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.unitsArray, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                updateConversion();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        //Remove the underlines
        TextView info = (TextView) topView.findViewById(R.id.infoText);
        info.setPaintFlags(info.getPaintFlags() & ~Paint.UNDERLINE_TEXT_FLAG);

        TextView alamoUnits = (TextView) topView.findViewById(R.id.alamoUnitsText);
        alamoUnits.setPaintFlags(alamoUnits.getPaintFlags() & ~Paint.UNDERLINE_TEXT_FLAG);

        updateConversion();
    }
    private void updateConversion(){
        //Get current spinner data
        Spinner spin = this.findViewById(R.id.unitsSpinner);
        setUnits((String)spin.getSelectedItem());
        //Get current time
        Time currTime = new Time();
        currTime.setToNow();
        long currTimeMillis = (new GregorianCalendar(currTime.year,currTime.month,
                currTime.monthDay,currTime.hour,currTime.minute)).getTimeInMillis();


        //Get current location
        Location loc;
        while(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = {Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION};
            ActivityCompat.requestPermissions(this,permissions,1);

        }
        loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        double[] currLoc = {loc.getLatitude(),loc.getLongitude(),loc.getAltitude()};

        //Do the maths
        long timeConv = currTimeMillis-alamoTime;
        double massConv = alamoMass+0;//add zero to remove complaint by code checker
        double[] diff = vecDiff(lla2ecef(currLoc),lla2ecef(alamoLoc));
        double[] currLocLLA = lla2ecef(currLoc);
        double[] alamoLocLLA = lla2ecef(alamoLoc);
        double distConv = norm( vecDiff(lla2ecef(currLoc),lla2ecef(alamoLoc)));
        double conversion = 1* pow(timeConv,s)* pow(massConv,kg)*pow(distConv,m);
        //Set text
        TextView outText = this.findViewById(R.id.alamoUnitsText);
        String[] outArr = ((""+outText.getText())).split("=");
        String outStr  = outArr[0]+"= "+conversion;
        outText.setText(outStr);//TODO: Add units (i.e. kg*m/s) to the string
    }
    private void setUnits(String units){
        switch(units){
            case "Distance":
                kg=0;
                s=0;
                m=1;
                break;
            case "Time":
                kg=0;
                s=1;
                m=0;
                break;
            case "Mass":
                kg=1;
                s=0;
                m=0;
                break;
            case "Velocity":
                kg=0;
                s=-1;
                m=1;
                break;
            case "Acceleration":
                kg=0;
                s=-2;
                m=1;
                break;
            case "Force":
                kg=1;
                s=-2;
                m=1;
                break;
            case "Energy":
                kg=1;
                s=-2;
                m=2;
                break;
            case "Density":
                kg=1;
                s=0;
                m=-3;
                break;
            case "Pressure":
                kg=1;
                s=0;
                m=-2;
                break;
            default:
                kg=0;
                s=0;
                m=1;
        }
    }
    private double[] lla2ecef(double[] lla){
        //wgs84 ellipsoid
        double lat=lla[0]*PI/180;
        double lon=lla[1]*PI/180;
        double alt=lla[2];
        long a=6378137;
        long b=6356752;
        double rs=1.0*a*b/(sqrt(b*b*pow(cos(lat),2)+a*a*pow(sin(lat),2)));
        double lambda=atan2(b*b* sin(lat),a*a* cos(lat));

        double[] r={rs*cos(lambda)*cos(lon)+alt*cos(lat)*cos(lon),
        rs*cos(lambda)*sin(lon)+alt*cos(lat)*sin(lon),
        rs*sin(lambda)+alt*sin(lat)};

        return r;
    }
    private double norm(double[] arr){
        double sum = 0;
        for(double d:arr){
            sum+=d*d;
        }
        return sqrt(sum);
    }
    private double[] vecDiff(double[] arr1, double[] arr2) throws ArithmeticException{
        double[] toReturn = new double[arr1.length];
        for(int i=0;i<toReturn.length;i++){
            toReturn[i]=arr1[i]-arr2[i];
        }
        return toReturn;
    }
}
