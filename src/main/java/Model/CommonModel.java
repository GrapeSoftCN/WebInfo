package Model;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import apps.appsProxy;
import database.dbFilter;
import nlogger.nlogger;
import string.StringHelper;

public class CommonModel {
    /**
     * 整合参数，将JSONObject类型的参数封装成JSONArray类型
     * 
     * @param object
     * @return
     */
    public JSONArray buildCond(String Info) {
        String key;
        Object value;
        JSONArray condArray = null;
        JSONObject object = JSONObject.toJSON(Info);
        dbFilter filter = new dbFilter();
        if (object != null && object.size() > 0) {
            for (Object object2 : object.keySet()) {
                key = object2.toString();
                value = object.get(key);
                filter.eq(key, value);
            }
            condArray = filter.build();
        } else {
            condArray = JSONArray.toJSONArray(Info);
        }
        return condArray;
    }

    /**
     * 获取图片相对路径
     * 
     * @param imageURL
     * @return
     */
    public String getImageUri(String imageURL) {
        int i = 0;
        if (imageURL.contains("File//upload")) {
            i = imageURL.toLowerCase().indexOf("file//upload");
            imageURL = "\\" + imageURL.substring(i);
        }
        if (imageURL.contains("File\\upload")) {
            i = imageURL.toLowerCase().indexOf("file\\upload");
            imageURL = "\\" + imageURL.substring(i);
        }
        if (imageURL.contains("File/upload")) {
            i = imageURL.toLowerCase().indexOf("file/upload");
            imageURL = "\\" + imageURL.substring(i);
        }
        return imageURL;
    }

    /**
     * 去除字符串中重复String类型的字符
     * 
     * @param tempString
     * @return
     */
    public static String removeSameString(String tempString) {
        String[] value = null;
        String rString = "";
        if (StringHelper.InvaildString(tempString)) {
            value = tempString.split(",");
        }
        if (value != null) {
            for (String wbid : value) {
                if (!rString.contains(wbid)) {
                    rString += wbid + ",";
                }
            }
        }
        return StringHelper.fixString(rString, ',');
    }

    /**
     * 去除字符串中指定String类型的字符
     * 
     * @param tempString
     * @return
     */
    public static String removeString(String tempString, String root) {
        String[] value = null;
        String rString = "";
        if (StringHelper.InvaildString(tempString)) {
            value = tempString.split(",");
        }
        if (value != null) {
            for (String wbid : value) {
                if (!wbid.equals(root)) {
                    rString += wbid + ",";
                }
            }
        }
        return StringHelper.fixString(rString, ',');
    }
    
    /**
     * 获取配置信息,获取configString中other数据
     * @param key
     * @return
     */
    public static String getconfig(String key) {
        String value = "";
        try {
            JSONObject object = JSONObject.toJSON(appsProxy.configValue().getString("other"));
            if (object!=null && object.size() > 0) {
                value = object.getString(key);
            }
        } catch (Exception e) {
            nlogger.logout(e);
            value = "";
        }
        return value;
    }
}
