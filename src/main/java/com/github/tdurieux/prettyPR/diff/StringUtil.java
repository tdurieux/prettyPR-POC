package com.github.tdurieux.prettyPR.diff;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.List;

public class StringUtil {
    public static List<String> strToListStr(String str) {
        return Arrays.asList(str.split("\n"));
    }

    public static String listStr2Str(List<String> list) {
        String content = null;
        for (int i = 0; i < list.size(); i++) {
            String s = list.get(i);
            if(content == null) {
                content = s;
            } else {
                content += "\n" + s;
            }
        }
        return content;
    }

    public static String downloadFile(URL url) {
        try {
            URLConnection urlConnection = url.openConnection();

            // open the stream and put it into BufferedReader
            BufferedReader br = new BufferedReader(
                                                          new InputStreamReader(urlConnection.getInputStream()));

            String content = null;
            String inputLine = "";
            while ((inputLine = br.readLine()) != null) {
                if(content == null) {
                    content = inputLine;
                } else {
                    content += "\n" + inputLine;
                }
            }
            br.close();
            return content;
        } catch (IOException e) {
            return null;
        }
    }
}
