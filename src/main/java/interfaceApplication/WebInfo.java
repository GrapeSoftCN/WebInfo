package interfaceApplication;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import JGrapeSystem.rMsg;
import Model.CommonModel;
import apps.appsProxy;
import authority.plvDef.plvType;
import interfaceModel.GrapeDBSpecField;
import interfaceModel.GrapeTreeDBModel;
import nlogger.nlogger;
import security.codec;
import session.session;
import string.StringHelper;
import time.TimeHelper;

public class WebInfo {
    private GrapeTreeDBModel web;
    private GrapeDBSpecField gDbSpecField;
    private CommonModel model;
    private session se;
    private JSONObject userInfo = null;
    private String currentWeb = null;
    private Integer userType = null;

    public WebInfo() {
        model = new CommonModel();

        web = new GrapeTreeDBModel();
        gDbSpecField = new GrapeDBSpecField();
        gDbSpecField.importDescription(appsProxy.tableConfig("WebInfo"));
        web.descriptionModel(gDbSpecField);
        web.bindApp();
        web.enableCheck();//开启权限检查

        se = new session();
        userInfo = se.getDatas();
        if (userInfo != null && userInfo.size() != 0) {
            currentWeb = userInfo.getString("currentWeb"); // 当前用户所属网站id
            userType =userInfo.getInt("userType");//当前用户身份
        }
        currentWeb = "597ff7609c93690f5a54291b";
    }

    /**
     * 添加站点信息 ，type 1 表示增加虚站，0 表示增加实站
     * 
     * @param webInfo
     *            （必填字段："host", "logo", "icp", "title"）
     * @return
     */
    @SuppressWarnings("unchecked")
    public String WebInsert(String webInfo) {
        String type = "0", linkId = "0", info;
        JSONObject obj = null;
        String result = rMsg.netMSG(100, "新增站点失败");
        webInfo = CheckParam(webInfo);
        if (webInfo.contains("errorcode")) {
            return webInfo;
        }
        JSONObject temp = JSONObject.toJSON(webInfo);
        JSONObject rMode = new JSONObject(plvType.chkType, plvType.powerVal).puts(plvType.chkVal, 100);//设置默认查询权限
    	JSONObject uMode = new JSONObject(plvType.chkType, plvType.powerVal).puts(plvType.chkVal, 200);
    	JSONObject dMode = new JSONObject(plvType.chkType, plvType.powerVal).puts(plvType.chkVal, 300);
    	temp.put("rMode", rMode.toJSONString()); //添加默认查看权限
    	temp.put("uMode", uMode.toJSONString()); //添加默认修改权限
    	temp.put("dMode", dMode.toJSONString()); //添加默认删除权限
        if (temp != null && temp.size() > 0) {
            if (temp.containsKey("type")) {
                type = temp.getString("type");
            }
            if (temp.containsKey("linkId")) {
                linkId = temp.getString("linkId");
            }
            switch (type) {
            case "1":
                // 验证绑定的站点，是否为虚拟站点
                linkId = CheckVwbid(linkId);
                if (linkId.contains("errorcode")) {
                    return linkId;
                }
                temp.put("title", TimeHelper.nowMillis());
            case "0":
                info = (String) web.data(temp).autoComplete().insertEx();
                obj = findbyid(info);
                break;
            }
            result = (obj != null && obj.size() > 0) ? rMsg.netMSG(0, obj) : result;
        }
        return result;
    }

    /**
     * 修改站点信息
     * 
     * @param wbid
     * @param WebInfo
     * @return
     */
    public String WebUpdate(String wbid, String WebInfo) {
        String result = rMsg.netMSG(100, "站点信息修改失败");
        boolean code = false;
        WebInfo = CheckParam(WebInfo);
        if (WebInfo.contains("errorcode")) {
            return WebInfo;
        }
        JSONObject object = JSONObject.toJSON(WebInfo);
        code = web.eq("_id", wbid).dataEx(object).updateEx();
        result = code ? rMsg.netMSG(0, "站点信息修改成功") : result;
        return result;
    }

    /**
     * 删除站点信息
     * 
     * @param wbid
     * @return
     */
    public String WebDelete(String wbid) {
        return WebBatchDelete(wbid);
    }

    public String WebBatchDelete(String wbid) {
        String[] values = null;
        long code = 0;
        String result = rMsg.netMSG(100, "站点信息删除失败");
        if (StringHelper.InvaildString(wbid)) {
            return rMsg.netMSG(99, "参数错误");
        }
        values = wbid.split(",");
        if (values != null) {
            web.or();
            for (String id : values) {
                web.eq("_id", id);
            }
            code = web.deleteAll();
            result = code != 0 ? rMsg.netMSG(0, "站点信息删除成功") : result;
        }

        return result;
    }

    /**
     * 分页 显示_id,title,wbgid,fatherid
     * 
     * @param idx
     * @param pageSize
     * @return
     */
    public String WebPage(int idx, int pageSize) {
        return WebPageBy(idx, pageSize, null);
    }

    public String WebPageBy(int idx, int pageSize, String webinfo) {
        return Page(idx, pageSize, webinfo, 0);
    }

    /**
     * 分页 显示所有字段 展示当前用户所属网站的所有下级站点
     * 
     * @param idx
     * @param pageSize
     * @return
     */
    public String WebPageBack(int idx, int pageSize) {
        return WebPageByBack(idx, pageSize, null);
    }

    public String WebPageByBack(int idx, int pageSize, String webinfo) {
        return Page(idx, pageSize, webinfo, 1);
    }

    /**
     * 分页操作
     * 
     * @param idx
     * @param pageSize
     * @param webinfo
     * @param type
     * @return
     */
    private String Page(int idx, int pageSize, String webinfo, int type) {
        String currentWId = "";
        long total = 0;
        JSONArray array = null;
        if (!StringHelper.InvaildString(currentWeb)) {
            return rMsg.netPAGE(idx, pageSize, total, new JSONArray());
        }
        currentWId = getWebTree(currentWeb); // 获取当前站点的所有子站点id，包含当前站点
        String[] values = currentWId.split(",");
        web.or();
        for (String string : values) {
            web.eq("_id", string);
        }
        if (StringHelper.InvaildString(webinfo)) {
            JSONArray condArray = model.buildCond(webinfo);
            if (condArray != null && condArray.size() > 0) {
                web.and().where(condArray);
            } else {
                return rMsg.netPAGE(idx, pageSize, total, new JSONArray());
            }
        }
        switch (type) {
        case 0: // 查询显示指定字段
            array = web.dirty().field("_id,title,wbgid,itemfatherID").page(idx, pageSize);
            break;
        case 1: // 查询显示所有字段
            array = web.dirty().page(idx, pageSize);
            break;
        }
        total = web.count();
        return rMsg.netPAGE(idx, pageSize, total, (array != null && array.size() > 0) ? array : new JSONArray());
    }

    /**
     * 获取当前站点的所有下级站点的信息，支持多个id
     * 
     * @project GrapeWebInfo
     * @package interfaceApplication
     * @file WebInfo.java
     * 
     * @param wbid
     * @return
     *
     */
    public String WebFindId(String wbid) {
        JSONArray array = Find(wbid);
        return (array != null && array.size() > 0) ? array.toJSONString() : new JSONArray().toJSONString();
    }

    public String WebFindByIds(String wbid) {
        JSONArray array = Find(wbid);
        return rMsg.netMSG(true, (array != null && array.size() > 0) ? array : new JSONArray());
    }

    private JSONArray Find(String wbid) {
        wbid = getChildWebids(wbid);
        String[] value = null;
        JSONArray array = null;
        if (!wbid.equals("")) {
            value = wbid.split(",");
        }
        if (value != null) {
            web.or();
            for (String string : value) {
                if (StringHelper.InvaildString(string)) {
                    web.eq("_id", string); 
                }
            }
            array = web.select();
        }
        return array = (array != null && array.size() > 0) ? v2r(array) : null;
    }

    /**
     * 获取当前站点的所有下级站点id，不包含本身id
     * 
     * @param root
     * @return
     */
    public String getChildWeb(String root) {
        String wbids = getWebTree(root);
        if (wbids.contains(root)) {
            wbids = CommonModel.removeString(wbids, root);
        }
        return wbids;
    }

    /**
     * 获取下级站点,包含当前站点，支持获取多个站点的下级站点id
     * 
     * @project GrapeWebInfo
     * @package interfaceApplication
     * @file WebInfo.java
     * 
     * @param wbid
     * @return
     *
     */
    private String getChildWebids(String wbid) {
        String rsString = "";
        String[] values = null;
        if (wbid != null && !wbid.equals("") && !wbid.equals("null")) {
            values = wbid.split(",");
        }
        if (values != null) {
            for (String string : values) {
                rsString += getWebTree(string) + ",";
            }
        }
        return CommonModel.removeSameString(rsString);
    }

    /**
     * 切换网站
     * 
     * @param wbid
     *            网站id
     * @return
     */
    @SuppressWarnings("unchecked")
    public String SwitchWeb(String wbid) {
        String result = rMsg.netMSG(100, "站点切换失败");
        if (userInfo != null && userInfo.size() > 0) {
            userInfo.put("currentWeb", wbid);
            userInfo = se.setDatas(userInfo);
            result = rMsg.netMSG(0, "站点切换成功");
        }
        return result;
    }

    /**
     * 新增虚站，验证虚站信息
     * 
     * @param vID
     * @return 要求：虚拟站点不允许与虚拟站点建立绑定关系，绑定的对象站点是否存在
     */
    private String CheckVwbid(String vID) {
        String linkId = null;
        JSONObject obj = null;
        String result = rMsg.netMSG(2, "虚站站点id错误");
        if (StringHelper.InvaildString(vID)) {
            obj = web.eq("_id", vID).find();
            if (obj != null && obj.size() != 0) {
                if (obj.containsKey("linkId")) {
                    linkId = obj.getString("linkId");
                    if (!linkId.equals("") && !linkId.equals("0")) {
                        return rMsg.netMSG(3, "虚拟站点不允许与虚拟站点建立绑定关系");
                    }
                }
                result = vID;
            } else {
                result = rMsg.netMSG(4, "绑定对象站点不存在");
            }
        }
        return result;
    }

    /**
     * 新增站点信息，修改站点信息，参数验证
     * 
     * @param Info
     * @return
     */
    @SuppressWarnings("unchecked")
    private String CheckParam(String Info) {
        String fatherid = "";
        String result = rMsg.netMSG(99, "参数错误");
        JSONObject webInfo = JSONObject.toJSON(Info);
        if (webInfo != null && webInfo.size() > 0) {
            try {
                if (webInfo.containsKey("thumbnail")) {
                    String image = webInfo.getString("thumbnail");
                    if (StringHelper.InvaildString(image)) {
                        image = model.getImageUri(codec.DecodeHtmlTag(image));
                        if (image != null) {
                            webInfo.put("thumbnail", image);
                        }
                    }
                }
                if (webInfo.containsKey("title") && webInfo.containsKey("fatherid")) {
                    String webname = webInfo.get("title").toString();
                    fatherid = webInfo.getString("fatherid");
                    if (findWebByTitle(fatherid, webname)) {
                        return rMsg.netMSG(1, "网站已存在");
                    }
                }
                result = Info;
            } catch (Exception e) {
                nlogger.logout(e);
                result = rMsg.netMSG(99, "参数错误");
            }
        }
        return result;
    }

    /**
     * 验证该网站是否已存在
     * 
     * @param name
     * @param fatherid
     * @return
     */
    private boolean findWebByTitle(String name, String fatherid) {
        JSONObject object = null;
        object = web.eq("title", name).eq("fatherid", fatherid).find();
        return object != null && object.size() > 0;
    }

    /**
     * 显示网站信息
     * 
     * @param wbid
     * @return
     */
    public JSONObject findbyid(String wbid) {
        JSONObject object = null;
        try {
            object = web.eq("_id", wbid).find();
        } catch (Exception e) {
            nlogger.logout(e);
            object = null;
        }
        return v2r(object); // 虚站转换为实战
    }

    // /**
    // * 获得当前网站节点树，包含自身及下级全部网站,获取单站点的下级全部网站
    // *
    // * @param root
    // * @return 返回信息含有虚站信息，则转换为实站信息
    // */
    // public String getWebTree(String root) {
    // String rsString = "";
    // rsString = getWebID(root, 1);
    // return rsString;
    // }

    // /**
    // * 获取所有子站点id，包含当前站点
    // *
    // * @project GrapeWebInfo
    // * @package interfaceApplication
    // * @file WebInfo.java
    // *
    // * @param root
    // * @return
    // *
    // */
    // public String getChild(String root) {
    // return rMsg.netMSG(0, getWebTree(root));
    // }

    /**
     * 前台获取全部子站点信息，包含虚站
     * 
     * @param idx
     * @param pageSize
     * @param root
     * @param wbgid
     * @return
     */
    public String getAllWeb(int idx, int pageSize, String root, String wbgid) {
        JSONArray data = web.eq("fatherid", root).select();
        JSONObject object;
        String tmpWbid, tempid = "";
        String rsString = "";
        for (Object obj : data) {
            object = (JSONObject) obj;
            tmpWbid = object.getMongoID("_id");
            tempid = getWeb(tmpWbid, wbgid);
            if (!tempid.equals("")) {
                rsString = rsString + "," + tempid;
            }
        }
        return getInfo(rsString, idx, pageSize);
    }

    /**
     * 查询社区或者村信息，根据不同的wbgid区分
     * 
     * @project GrapeWebInfo
     * @package model
     * @file WebModel.java
     * 
     * @param root
     * @param wbgid
     * @return
     *
     */
    private String getWeb(String root, String wbgid) {
        if (wbgid != null && !wbgid.equals("0") && !wbgid.equals("") && !wbgid.equals("null")) {
            web.eq("wbgid", wbgid);
        }
        JSONArray array = web.eq("fatherid", root).select();
        JSONObject object;
        String tmpWbid, tempid = "";
        String rsString = root;
        if (array == null || array.size() == 0) {
            return rsString;
        }
        for (Object obj : array) {
            object = (JSONObject) obj;
            tmpWbid = object.getMongoID("_id");
            tempid = getWeb(tmpWbid, wbgid);
            if (!tempid.equals("")) {
                rsString = rsString + "," + tempid;
            }
        }
        return StringHelper.fixString(rsString, ',');
    }

    /**
     * 批量获取网站信息
     * 
     * @param wbid
     * @param idx
     * @param pageSize
     * @return
     */
    private String getInfo(String wbid, int idx, int pageSize) {
        long total = 0;
        JSONArray array = null;
        if (!wbid.equals("")) {
            String[] wbids = wbid.split(",");
            for (String value : wbids) {
                if (StringHelper.InvaildString(value)) {
                    web.eq("_id", value);
                }
            }
            array = web.dirty().desc("sort").asc("_id").page(idx, pageSize);
            total = web.count();
        }
        return rMsg.netPAGE(idx, pageSize, total, (array != null && array.size() > 0) ? v2r(array) : new JSONArray());
    }

    /**
     * 获取所有上级网站id,包含自身id
     * 
     * 
     * @param root
     * @return wbid,wbid,wbid
     */
    public String getAllFatherWebID(String root) {
        String rsString = root;
        while (!root.equals("") && !root.equals("0")) {
            JSONArray data = v2r(web.eq("_id", root).select());
            JSONObject object;
            for (Object obj : data) {
                object = (JSONObject) obj;
                root = object.getString("itemfatherID");
                if (StringHelper.InvaildString(root) && !root.equals("0")) {
                    rsString += "," + root;
                }
            }
        }
        return StringHelper.fixString(rsString, ',');
    }

    /**
     * 获取所有上级网站id,不包含自身id
     * 
     * 
     * @param root
     * @return wbid,wbid,wbid
     */
    public String getFatherWebID(String root) {
        String rsString = getAllFatherWebID(root);
        return CommonModel.removeString(rsString, root);
    }

    /**
     * 获取全部父站点信息,不包含当前站点id
     * 
     * @param idx
     * @param pageSize
     * @param root
     * @return
     */
    public String getFatherWeb(int idx, int pageSize, String root) {
        String wbid = getFatherWebID(root); // 不包含自身网站
        return getInfo(wbid, idx, pageSize);
    }

    /**
     * 获取网站默认缩略图,文章后缀信息
     * 
     * @param wbid
     * @return
     */
    @SuppressWarnings("unchecked")
    public String getImage(String wbid) {
        String image = "", suffix = "",wid;
        String[] value = null;
        String url = CommonModel.getconfig("imageURL");
        JSONArray array = null;
        JSONObject objTemp,obj = null;
        if (StringHelper.InvaildString(wbid)) {
            value = wbid.split(",");
        }
        if (value!=null) {
            web.or();
            for (String id : value) {
                web.eq("_id", id);
            }
            array = web.field("_id,thumbnail,suffix").select();
        }
        if (array != null && array.size() != 0 ) {
            obj = new JSONObject();
            for (Object object : array) {
                objTemp = (JSONObject)object;
                wid = objTemp.getMongoID("_id");
                if (objTemp.containsKey("thumbnail")) {
                    image = objTemp.getString("thumbnail");
                    if (StringHelper.InvaildString(url) && StringHelper.InvaildString(image)) {
                        image = url + image;
                    }
                }
                if (objTemp.containsKey("suffix")) {
                    suffix = objTemp.getString("suffix");
                }
                objTemp.put("thumbnail", image);
                objTemp.put("suffix", suffix);
                obj.put(wid, objTemp);
            }
            
        }
        return (obj != null && obj.size() > 0) ? obj.toJSONString() : new JSONObject().toJSONString();
    }

    /**
     * 查询网站访问量
     * 
     * @project GrapeWebInfo
     * @package interfaceApplication
     * @file WebInfo.java
     * 
     * @param condString
     * @return
     *
     */
    @SuppressWarnings("unchecked")
    public String view(String condString) {
        JSONObject object = null;
        String temp = "";
        JSONArray condArray = JSONArray.toJSONArray(condString);
        if (condArray != null && condArray.size() != 0) {
            object = web.where(condArray).field("allno").find();
        }
        if (object != null && object.size() != 0) {
            temp = object.getString("allno");
            if (temp.contains("$numberLong")) {
                temp = JSONObject.toJSON(temp).getString("$numberLong");
            }
            object.put("allno", Integer.parseInt(temp));
        }
        return rMsg.netMSG(true, (object != null && object.size() > 0) ? object : new JSONObject());
    }

    /**
     * 网站访问量增加，即allno+1
     * 
     * @project GrapeWebInfo
     * @package interfaceApplication
     * @file WebInfo.java
     * 
     * @param condString
     * @return
     *
     */
    public String viewCount(String condString) {
        int code = 99;
        String data = "{\"allno\":0}";
        String result = rMsg.netMSG(100, "新增访问量失败");
        JSONArray condArray = JSONArray.toJSONArray(condString);
        try {
            if (condArray != null && condArray.size() != 0) {
                int count = getCount(condArray);
                data = "{\"allno\":" + count + "}";
                code = web.where(condArray).data(data).update() != null ? 0 : 99;
            }
            result = code == 0 ? rMsg.netMSG(0, "新增访问量成功") : result;
        } catch (Exception e) {
            nlogger.logout(e);
            code = 100;
        }
        return result;
    }

    /**
     * 当前网站的统计量 +1
     * 
     * @project GrapeWebInfo
     * @package interfaceApplication
     * @file WebInfo.java
     * 
     * @param condArray
     * @return
     *
     */
    private int getCount(JSONArray condArray) {
        int count = 0;
        try {
            JSONObject object = web.where(condArray).field("allno").limit(1).find();
            if (object != null && object.size() != 0) {
                if (object.containsKey("allno")) {
                    String counts = object.getString("allno");
                    if (counts.contains("$numberLong")) {
                        counts = JSONObject.toJSON(counts).getString("$numberLong");
                    }
                    count = Integer.parseInt(counts);
                }
            }
        } catch (Exception e) {
            nlogger.logout(e);
            count = 0;
        }
        return count + 1;
    }

    /**
     * 获取当前站点下的所有子站点id，同时包含当前站点
     * 
     * @param root
     * @return wbid,wbid,wbid
     */
    public String getWebTree(String root) {
        String rString = root;
        JSONArray itemArray = null;
        if (StringHelper.InvaildString(root)) {
            JSONObject object = web.eq("_id", root).getAllChildren();
            if (object != null && object.size() > 0) {
                itemArray = object.getJsonArray("itemChildrenData");
                if (itemArray != null && itemArray.size() > 0) {
                    for (Object obj : itemArray) {
                        rString = rString + "," + getChildID((JSONObject) obj);
                    }
                }
            }
        }
        return CommonModel.removeSameString(rString);
    }

    /**
     * 获取所有子站点id
     * 
     * @param objects
     * @return
     */
    private String getChildID(JSONObject objects) {
        JSONArray item = null;
        String rString = "", tempID;
        JSONObject object;
        if (objects != null && objects.size() > 0) {
            item = objects.getJsonArray("itemChildrenData");
            item = (item == null || item.size() <= 0) ? null : item;
            rString += objects.getMongoID("_id") + ",";
            if (item != null) {
                for (Object obj : item) {
                    object = (JSONObject) obj;
                    tempID = getChildID(object);
                    if (StringHelper.InvaildString(tempID) && !rString.contains(tempID)) {
                        rString += tempID + ",";
                    }
                }
            }
        }
        return StringHelper.fixString(rString, ',');
    }

    /**
     * 虚站转实战
     * 
     * @param json
     * @return
     */
    @SuppressWarnings("unchecked")
    private JSONObject v2r(JSONObject json) {
        String realID;
        JSONObject rJson;
        rJson = json;
        if (json == null || json.size() <= 0) {
            return new JSONObject();
        }
        if (json.getString("type").equals("1")) {// 虚站
            realID = json.getString("linkId");
            JSONArray wbInfo = getWebsInfo(realID);
            if (wbInfo != null && wbInfo.size() > 0) {
                rJson = (JSONObject) wbInfo.get(0);
                rJson.put("_id", json.getMongoID("_id"));
                rJson.put("itemfatherID", json.getString("itemfatherID"));
                rJson.put("vwState", 1);
            }
        }
        return rJson;
    }

    private static JSONArray v2r(JSONArray array) {
        return v2r(array, false);
    }

    @SuppressWarnings("unchecked")
    private static JSONArray v2r(JSONArray array, boolean simple) {
        String realID, cmdString = "";
        JSONObject json;
        JSONObject taskCache = new JSONObject();
        JSONObject infoCache = new JSONObject();
        JSONArray newArray = new JSONArray();
        if (array == null || array.size() == 0) {
            return newArray;
        }
        for (Object obj : array) {
            json = (JSONObject) obj;
            if (json.getString("type").equals("1")) {// 虚站
                realID = json.getString("linkId");
                taskCache.put(realID, ((JSONObject) json.get("_id")).getString("$oid"));
                infoCache.put(realID, json.get("fatherid"));
                cmdString += realID + ",";
            } else {// 记录实站数据
                newArray.add(obj);
            }
        }
        if (!cmdString.equals("")) {
            String vID;
            JSONObject oid;
            cmdString = StringHelper.fixString(cmdString, ',');
            WebInfo wb = new WebInfo();
            JSONArray wbInfo = wb.getWebsInfo(cmdString);
            for (Object obj : wbInfo) {
                json = (JSONObject) obj;
                if (!simple) {
                    oid = (JSONObject) json.get("_id");
                    realID = oid.getString("$oid");// 获得实站ID
                    vID = taskCache.getString(realID);// 获得虚站ID
                    oid.put("$oid", vID);
                    json.put("_id", oid);// 使用实站数据代替虚站数据
                    json.put("fatherid", infoCache.getString(realID));
                }
                json.put("vwState", 1);
                newArray.add(json);
            }
        }
        return newArray;
    }

    /**
     * 获取站点信息
     * 
     * @param wbid
     * @return
     */
    public JSONArray getWebsInfo(String wbid) {
        wbid = VID2RID(wbid);
        String[] wbids = wbid.split(",");
        web.or();
        for (String value : wbids) {
            if (value != null && !value.equals("")) {
                web.eq("_id", value);
            }
        }
        return web.select();
    }
    /**
     * 批量查询网站名称
     * 
     * @project GrapeWebInfo
     * @package interfaceApplication
     * @file WebInfo.java
     * 
     * @param wbid
     * @return {wbid:title,wbid:title}
     *
     */
    @SuppressWarnings("unchecked")
    public String getWebInfo(String wbid) {
        JSONObject object = new JSONObject(),obj;
        JSONArray array = getWebsInfo(wbid);
        if ((array != null) && (array.size() != 0)) {
            for (Object object2 : array) {
                obj = (JSONObject)object2;
                String wid = ((JSONObject) obj.get("_id")).getString("$oid");
                String title = obj.getString("title");
                object.put(wid, title);
            }
        }
        return object.toJSONString();
    }
    /**
     * 获取实站id
     * 
     * @project GrapeWebInfo
     * @package interfaceApplication
     * @file WebInfo.java
     * 
     * @param wbid
     * @return
     *
     */
    public String VID2RID(String wbid) {
        String type = "0", linkId;
        JSONObject object = web.eq("_id", wbid).find();
        if (object != null && object.size() != 0) {
            if (object.containsKey("type")) {
                type = object.getString("type");
            }
            if (type.equals("1")) {
                if (object.containsKey("linkId")) {
                    linkId = object.getString("linkId");
                    wbid = (!linkId.equals("") && !linkId.equals("0")) ? linkId : wbid;
                }
            }
        }
        return wbid;
    }
}
