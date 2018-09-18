package com.squalala.chatapp;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Lenovo on 1/29/2018.
 */

public class JSONParserTask {
    String time=null;
    String distance=null;

    public List<List<HashMap<String,String>>> parse(JSONObject jObject){

        List<List<HashMap<String, String>>> routes = new ArrayList<>() ;
        JSONArray jRoutes;
        JSONArray jLegs;
        JSONArray jSteps;

        try {
         /*   JSONArray routeArray = jObject.getJSONArray("routes");
            JSONObject routeObject = routeArray.getJSONObject(0);
            JSONArray legsArray = routeObject.getJSONArray("legs");
            JSONObject timeandDistanceObj = legsArray.getJSONObject(0);
            JSONObject timeobj = timeandDistanceObj.getJSONObject("duration");
            JSONObject distanceobj = timeandDistanceObj.getJSONObject("distance");
            Log.d("distance&time",timeobj.getString("text") + distanceobj.getString("text"));*/
            jRoutes = jObject.getJSONArray("routes");
            for(int i=0;i<jRoutes.length();i++){
                jLegs = ( (JSONObject)jRoutes.get(i)).getJSONArray("legs");

                String totaltime;
                List path = new ArrayList<>();

                for(int j=0;j<jLegs.length();j++){
                    jSteps = ( (JSONObject)jLegs.get(j)).getJSONArray("steps");

                    for(int k=0;k<jSteps.length();k++){
                        String polyline = "";
                        polyline = (String)((JSONObject)((JSONObject)jSteps.get(k)).get("polyline")).get("points");
                        List<LatLng> list = decodePoly(polyline);

                        for(int l=0;l<list.size();l++){
                            HashMap<String, String> hm = new HashMap<>();
                            hm.put("lat", Double.toString((list.get(l)).latitude) );
                            hm.put("lng", Double.toString((list.get(l)).longitude) );
                            path.add(hm);
                        }
                    }
                    routes.add(path);
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }catch (Exception e){
            Log.e("error",e.toString());
        }


        return routes;
    }
    public String getTime()
    {
        return time;
    }
    public String getDistance()
    {
        return distance;
    }
    private List<LatLng> decodePoly(String encoded) {

        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }
        return poly;
    }
}