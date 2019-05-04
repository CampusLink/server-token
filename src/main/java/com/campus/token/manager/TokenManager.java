package com.campus.token.manager;

import com.alibaba.fastjson.JSONObject;
import com.campus.system.ServiceContext;
import com.campus.system.ServiceMenu;
import com.campus.system.storage.StorageService;
import com.campus.system.storage.model.Box;
import com.campus.system.storage.model.BoxQuery;
import com.campus.system.storage.model.BoxStore;
import com.campus.system.storage.model.StorageType;
import com.campus.system.token.model.SecretShip;
import com.campus.system.token.model.Token;
import com.campus.token.constant.TokenConst;
import com.campus.token.util.DesUtil;

import java.util.HashMap;
import java.util.List;

public class TokenManager {
    private HashMap<String, Token> mCache;
    private final int MAX_CACHE = 1000;
    private final int EXPIRE_TIME = 1000 * 60 * 60 * 24 * 30;
    private ServiceContext mContext;
    private Box<Token> mTokenBox;
    private Box<SecretShip> mSecretShipBox;
    private final String storeTokenName = "token";
    private final String storeSecretName = "secret_ship";
    private final String userName = "username";
    private final String password = "password";

    private static class Holder {
        static TokenManager sInstance = new TokenManager();
    }

    public TokenManager() {
        mCache = new HashMap<String, Token>(MAX_CACHE);
    }

    public static TokenManager getInstance() {
        return Holder.sInstance;
    }

    public synchronized void init(ServiceContext context) {
        if (mContext != null) {
            return;
        }
        mContext = context;
        StorageService storage = (StorageService) context.getSystemService(ServiceMenu.STORAGE);
        BoxStore store = storage.obtainBoxStore(StorageType.MySql, storeTokenName, userName, password);
        mTokenBox = store.boxFor(Token.class);
        store = storage.obtainBoxStore(StorageType.MySql, storeSecretName, userName, password);
        mSecretShipBox = store.boxFor(SecretShip.class);
    }

    public Token getToken(String token) {
        if (mCache.containsKey(token)) {
            return mCache.get(token);
        }
        try {
            String tokenPublicStr = DesUtil.decrypt(token, TokenConst.publicSecret);
            List<SecretShip> ships = mSecretShipBox.obtainQuery()
                    .whereEqualTo("token", tokenPublicStr)
                    .limit(1)
                    .query();
            if(ships != null && ships.size() > 0){
                SecretShip ship = ships.get(0);
                String tokenStr = DesUtil.decrypt(tokenPublicStr, ship.getSecret());
                Token tokenObj = JSONObject.parseObject(tokenStr, Token.class);
                mCache.put(token, tokenObj);
                return tokenObj;
            }else{
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    public boolean verifyToken(String token){
        Token tokenObj = getToken(token);
        if(tokenObj == null){
            return false;
        }
        if(System.currentTimeMillis() - tokenObj.getCreateTime() > tokenObj.getExpire()){
            mCache.remove(token);
            return false;
        }
        return true;
    }

    public Token updateToken(String userId){
        List<SecretShip> ships = mSecretShipBox.obtainQuery()
                .whereEqualTo("userId", userId)
                .limit(1)
                .query();
        if(ships == null || ships.size() == 0){
            return null;
        }

        SecretShip ship = ships.get(0);
        Token token = new Token();
        token.setCreateTime(System.currentTimeMillis());
        token.setExpire(EXPIRE_TIME);
        token.setUserId(userId);

        try {
            String tokenStr = DesUtil.encrypt(JSONObject.toJSONString(token), ship.getSecret());
            ship.setToken(tokenStr);
            mSecretShipBox.put(ship);
            String tokenPublic = DesUtil.encrypt(tokenStr, TokenConst.publicSecret);
            mCache.put(tokenPublic, token);
            return token;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public Token  createToken(String userId){
        return updateToken(userId);
    }
}
