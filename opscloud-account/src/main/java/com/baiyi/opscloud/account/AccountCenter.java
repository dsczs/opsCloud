package com.baiyi.opscloud.account;

import com.baiyi.opscloud.account.factory.AccountFactory;
import com.baiyi.opscloud.common.util.UUIDUtils;
import com.baiyi.opscloud.domain.BusinessWrapper;
import com.baiyi.opscloud.domain.ErrorEnum;
import com.baiyi.opscloud.domain.generator.opscloud.OcUser;
import com.baiyi.opscloud.domain.param.auth.LogParam;
import com.baiyi.opscloud.domain.vo.auth.LogVO;
import com.baiyi.opscloud.facade.OcAuthFacade;
import com.baiyi.opscloud.ldap.credential.PersonCredential;
import com.baiyi.opscloud.ldap.handler.LdapHandler;
import com.baiyi.opscloud.service.user.OcUserService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;

/**
 * @Author baiyi
 * @Date 2020/1/8 8:06 下午
 * @Version 1.0
 */
@Component("AccountCenter")
public class AccountCenter {

    @Resource
    private LdapHandler ldapHandler;

    @Resource
    private OcAuthFacade ocAuthFacade;

    @Resource
    private OcUserService ocUserService;

    public static final String LDAP_ACCOUNT_KEY = "LdapAccount";

    /**
     * 登录接口
     * @param loginParam
     * @return
     */
    public BusinessWrapper<Boolean> loginCheck(LogParam.LoginParam loginParam) {
        com.baiyi.opscloud.ldap.credential.PersonCredential credential = PersonCredential.builder()
                .username(loginParam.getUsername())
                .password(loginParam.getPassword())
                .build();
        // 验证通过
        if (ldapHandler.loginCheck(credential)) {
            String token = UUIDUtils.getUUID();
            ocAuthFacade.setUserToken(loginParam.getUsername(), token);
            OcUser ocUser = ocUserService.queryOcUserByUsername(loginParam.getUsername());
            LogVO.LoginVO loginVO = new LogVO.LoginVO();
            loginVO.setName(ocUser.getDisplayName());
            loginVO.setUuid(ocUser.getUuid());
            loginVO.setToken(token);
            ocAuthFacade.setOcUserPassword(ocUser, loginParam.getPassword());
            BusinessWrapper wrapper = BusinessWrapper.SUCCESS;
            wrapper.setBody(loginVO);
            return wrapper;
        } else {
            return new BusinessWrapper(ErrorEnum.USER_LOGIN_FAILUER);
        }
    }

    public Boolean create(String key, OcUser user) {
        IAccount account = AccountFactory.getAccountByKey(key);
        return account.create(user);
    }

    public Boolean create(OcUser user) {
        Boolean result = create(LDAP_ACCOUNT_KEY, user);
        if (result) {
            Map<String, IAccount> accountContainer = AccountFactory.getAccountContainer();
            for (String key : accountContainer.keySet()) {
                if (key.equals(LDAP_ACCOUNT_KEY)) continue;
                IAccount account = accountContainer.get(key);
                if (!account.create(user))
                    return Boolean.FALSE;
            }
        }
        return Boolean.TRUE;
    }

    /**
     * 授权
     *
     * @param user
     * @param resource
     * @return
     */
    public Boolean grant(OcUser user, String resource) {
        Map<String, IAccount> accountContainer = AccountFactory.getAccountContainer();
        for (String key : accountContainer.keySet()) {
            if (key.equals(LDAP_ACCOUNT_KEY)) continue;
            IAccount account = accountContainer.get(key);
            if (!account.grant(user, resource))
                return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    /**
     * 撤销授权
     *
     * @param user
     * @param resource
     * @return
     */
    public Boolean revoke(OcUser user, String resource) {
        Map<String, IAccount> accountContainer = AccountFactory.getAccountContainer();
        for (String key : accountContainer.keySet()) {
            if (key.equals(LDAP_ACCOUNT_KEY)) continue;
            IAccount account = accountContainer.get(key);
            if (!account.revoke(user, resource))
                return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }


    public Boolean update(String key, OcUser user) {
        IAccount account = AccountFactory.getAccountByKey(key);
        return account.update(user);
    }

    /**
     * 更新用户信息
     *
     * @param user
     * @return
     */
    public Boolean update(OcUser user) {
        Boolean result = update(LDAP_ACCOUNT_KEY, user);
        if (result) {
            Map<String, IAccount> accountContainer = AccountFactory.getAccountContainer();
            for (String key : accountContainer.keySet()) {
                if (key.equals(LDAP_ACCOUNT_KEY)) continue;
                IAccount account = accountContainer.get(key);
                if (!account.update(user))
                    return Boolean.FALSE;
            }
        }
        return Boolean.TRUE;
    }

    public Boolean pushSSHKey(OcUser user) {
        Map<String, IAccount> accountContainer = AccountFactory.getAccountContainer();
        for (String key : accountContainer.keySet()) {
            if (key.equals(LDAP_ACCOUNT_KEY)) continue;
            IAccount account = accountContainer.get(key);
            if (!account.pushSSHKey(user))
                return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    public Boolean sync(String key) {
        IAccount account = AccountFactory.getAccountByKey(key);
        return account.sync();
    }

}
