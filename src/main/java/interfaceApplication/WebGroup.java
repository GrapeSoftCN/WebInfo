package interfaceApplication;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import JGrapeSystem.rMsg;
import Model.CommonModel;
import apps.appsProxy;
import authority.plvDef.plvType;
import interfaceModel.GrapeDBSpecField;
import interfaceModel.GrapeTreeDBModel;
import json.JSONHelper;
import string.StringHelper;

public class WebGroup {
    private GrapeTreeDBModel group;
    private GrapeDBSpecField gDbSpecField;
    private CommonModel model;
    private Integer userType = null;

    public WebGroup() {
        model = new CommonModel();

        group = new GrapeTreeDBModel();
        gDbSpecField = new GrapeDBSpecField();
        gDbSpecField.importDescription(appsProxy.tableConfig("WebGroup"));
        group.descriptionModel(gDbSpecField);
        group.bindApp();
        group.enableCheck();//开启权限检查
    }

    /**
     * 新增站群
     * 
     * @param webgroupInfo
     * @return
     */
    public String WebGroupInsert(String webgroupInfo) {
        String result = rMsg.netMSG(100, "新增站群失败");
        Object code = 99;
        if (StringHelper.InvaildString(webgroupInfo)) {
            JSONObject groupInfo = JSONObject.toJSON(webgroupInfo);
            // 判断库中是否存在同名站群
            String name = groupInfo.getString("name");
            if (findByName(name)) {
                return rMsg.netMSG(1, "站群已存在"); // 站群已存在
            }
            JSONObject rMode = new JSONObject(plvType.chkType, plvType.powerVal).puts(plvType.chkVal, 100);//设置默认查询权限
        	JSONObject uMode = new JSONObject(plvType.chkType, plvType.powerVal).puts(plvType.chkVal, 200);
        	JSONObject dMode = new JSONObject(plvType.chkType, plvType.powerVal).puts(plvType.chkVal, 300);
        	groupInfo.put("rMode", rMode.toJSONString()); //添加默认查看权限
        	groupInfo.put("uMode", uMode.toJSONString()); //添加默认修改权限
        	groupInfo.put("dMode", dMode.toJSONString()); //添加默认删除权限
            
            code = group.data(groupInfo).autoComplete().insertEx();
        }
        result = code != null ? rMsg.netMSG(0, "新增站群成功") : result;
        return result;
    }

    /**
     * 搜索
     * 
     * @param webinfo
     * @return
     */
    public String WebGroupFind(String webinfo) {
        JSONArray array = null;
        if (StringHelper.InvaildString(webinfo)) {
            JSONArray condArray = model.buildCond(webinfo);
            if (condArray != null && condArray.size() > 0) {
                array = group.where(condArray).select();
            }
        }
        return rMsg.netMSG(true, (array != null && array.size() > 0) ? array : new JSONArray());
    }

    /**
     * 查询站群信息
     * 
     * @param wbid
     * @return
     */
    public String WebGroupFindBywbid(String wbid) {
        JSONObject object = null;
        object = group.eq("_id", wbid).find();
        return rMsg.netMSG(0, (object != null && object.size() > 0) ? object : new JSONObject());
    }

    /**
     * 修改站群信息
     * 
     * @param wbgid
     * @param webgroupInfo
     * @return
     */
    public String WebGroupUpdate(String wbgid, String webgroupInfo) {
        String result = rMsg.netMSG(100, "站群修改失败");
        Object code = 99;
        JSONObject _webinfo = JSONHelper.string2json(webgroupInfo);
        if (_webinfo != null && _webinfo.size() > 0) {
            if (_webinfo.containsKey("name")) {
                String name = _webinfo.get("name").toString();
                if (findByName(name)) {
                    return rMsg.netMSG(1, "该站群已存在"); // 站群已存在
                }
            }
            code = group.eq("_id", wbgid).data(_webinfo).updateEx();
            result = code != null ? rMsg.netMSG(0, "站群修改成功") : result;
        }
        return result;
    }

    /**
     * 分页显示
     * 
     * @param idx
     * @param pageSize
     * @return
     */
    public String WebGroupPage(int idx, int pageSize) {
        return WebGroupPageBy(idx, pageSize, null);
    }

    public String WebGroupPageBy(int idx, int pageSize, String webinfo) {
        long total = 0;
        JSONArray array = null;
        if (!StringHelper.InvaildString(webinfo)) {
            JSONArray condArray = model.buildCond(webinfo);
            if (condArray != null && condArray.size() > 0) {
                group.where(condArray);
            } else {
                return rMsg.netPAGE(idx, pageSize, total, new JSONArray());
            }
        }
        array = group.page(idx, pageSize);
        return rMsg.netPAGE(idx, pageSize, total, (array != null && array.size() > 0) ? array : new JSONArray());
    }

    /**
     * 验证网站是否存在于表中
     * 
     * @param name
     * @return true 表中已存在
     */
    private boolean findByName(String name) {
        JSONObject object = group.eq("name", name).find();
        return object != null && object.size() > 0;
    }
}
