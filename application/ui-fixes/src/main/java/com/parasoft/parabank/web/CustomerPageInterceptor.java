package com.parasoft.parabank.web;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.dao.DataAccessException;
import org.springframework.web.servlet.ModelAndViewDefiningException;
import org.springframework.web.util.WebUtils;
import com.parasoft.parabank.domain.Account;
import com.parasoft.parabank.domain.logic.BankManager;
import com.parasoft.parabank.util.Constants;

/** Authenticate customer pages before resource lookup; reject non-owned resources. */
public class CustomerPageInterceptor extends LoginInterceptor {
    @Resource(name = "bankManager")
    private BankManager bankManager;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
        super.preHandle(request, response, handler);
        String path = request.getServletPath();
        if ("/activity.htm".equals(path) || "/transaction.htm".equals(path)) {
            UserSession session = (UserSession) WebUtils.getSessionAttribute(request, Constants.USERSESSION);
            boolean owned = false;
            try {
                int id = Integer.parseInt(request.getParameter("id"));
                int accountId = "/transaction.htm".equals(path)
                    ? bankManager.getTransaction(id).getAccountId() : id;
                Account account = bankManager.getAccount(accountId);
                owned = account != null && account.getCustomerId() == session.getCustomer().getId();
            } catch (NumberFormatException | DataAccessException | NullPointerException invalidResource) {
                // Identical response for missing and non-owned resources prevents ID enumeration.
            }
            if (!owned) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                throw new ModelAndViewDefiningException(ViewUtil.createErrorView("error.resource.access"));
            }
        }
        return true;
    }
}
