package com.campus.token;

import com.campus.system.ServiceContext;
import com.campus.system.ServiceMenu;
import com.campus.system.annotation.Service;
import com.campus.system.token.TokenService;
import com.campus.system.token.model.Token;
import com.campus.token.manager.TokenManager;

@Service(name = ServiceMenu.TOKEN, module = "Token")
public class TokenServiceImpl extends TokenService {
    public void init(ServiceContext serviceContext) {
        TokenManager.getInstance().init(serviceContext);
    }

    public Token createToken(String userId) {
        return TokenManager.getInstance().createToken(userId);
    }

    public Token parseToken(String token) {
        return TokenManager.getInstance().getToken(token);
    }

    public Token updateToken(String userId) {

        return TokenManager.getInstance().updateToken(userId);
    }

    public boolean verifyToken(String token) {
        return TokenManager.getInstance().verifyToken(token);
    }
}
