<%@ include file="../include/include.jsp" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<h1 class="title"><fmt:message key="signing.up"/></h1>

<p><fmt:message key="personal.info"/></p>

<form:form method="post" action="register.htm" modelAttribute="customerForm" >
  <table class="form2" >
    <tr>
      <td align="right" width="30%"><b><label for="customer.firstName"><fmt:message key="first.name"/>:</label></b></td>
      <td width="20%">
        <form:input cssClass="input" path="customer.firstName"/>
      </td>
      <td width="50%">
        <form:errors path="customer.firstName" cssClass="error"/>
      </td>
    </tr>
    <tr>
      <td align="right" width="30%"><b><label for="customer.lastName"><fmt:message key="last.name"/>:</label></b></td>
      <td width="20%">
        <form:input cssClass="input" path="customer.lastName"/>
      </td>
      <td width="50%">
        <form:errors path="customer.lastName" cssClass="error"/>
      </td>
    </tr>
    <tr>
      <td align="right" width="30%"><b><label for="customer.address.street"><fmt:message key="address"/>:</label></b></td>
      <td width="20%">
        <form:input cssClass="input" path="customer.address.street"/>
      </td>
      <td width="50%">
        <form:errors path="customer.address.street" cssClass="error"/>
      </td>
    </tr>
    <tr>
      <td align="right" width="30%"><b><label for="customer.address.city"><fmt:message key="city"/>:</label></b></td>
      <td width="20%">
        <form:input cssClass="input" path="customer.address.city"/>
      </td>
      <td width="50%">
        <form:errors path="customer.address.city" cssClass="error"/>
      </td>
    </tr>
    <tr>
      <td align="right" width="30%"><b><label for="customer.address.state"><fmt:message key="state"/>:</label></b></td>
      <td width="20%">
        <form:input cssClass="input" path="customer.address.state"/>
      </td>
      <td width="50%">
        <form:errors path="customer.address.state" cssClass="error"/>
      </td>
    </tr>
    <tr>
      <td align="right" width="30%"><b><label for="customer.address.zipCode"><fmt:message key="zip.code"/>:</label></b></td>
      <td width="20%">
        <form:input cssClass="input" path="customer.address.zipCode"/>
      </td>
      <td width="50%">
        <form:errors path="customer.address.zipCode" cssClass="error"/>
      </td>
    </tr>
    <tr>
      <td align="right" width="30%"><b><label for="customer.phoneNumber"><fmt:message key="phone.number"/>:</label></b></td>
      <td width="20%">
        <form:input cssClass="input" path="customer.phoneNumber"/>
      </td>
      <td width="50%">
        <form:errors path="customer.phoneNumber" cssClass="error"/>
      </td>
    </tr>
    <tr>
      <td align="right" width="30%"><b><label for="customer.ssn"><fmt:message key="ssn"/>:</label></b></td>
      <td width="20%">
        <form:input cssClass="input" path="customer.ssn"/>
      </td>
      <td width="50%">
        <form:errors path="customer.ssn" cssClass="error"/>
      </td>
    </tr>
    <tr><td>&nbsp;</td></tr>
    <tr>
      <td align="right" width="30%"><b><label for="customer.username"><fmt:message key="username"/>:</label></b></td>
      <td width="20%">
        <form:input cssClass="input" path="customer.username"/>
      </td>
      <td width="50%">
        <form:errors path="customer.username" cssClass="error"/>
      </td>
    </tr>
    <tr>
      <td align="right" width="30%"><b><label for="customer.password"><fmt:message key="password"/>:</label></b></td>
      <td width="20%">
        <form:password cssClass="input" path="customer.password"/>
      </td>
      <td width="50%">
        <form:errors path="customer.password" cssClass="error"/>
      </td>
    </tr>
    <tr>
      <td align="right" width="30%"><b><label for="repeatedPassword"><fmt:message key="repeated.password"/>:</label></b></td>
      <td width="20%">
        <form:password cssClass="input" path="repeatedPassword"/>
      </td>
      <td width="50%">
        <form:errors path="repeatedPassword" cssClass="error"/>
      </td>
    </tr>    
    <tr>
      <td>&nbsp;</td>
      <td colspan="2"><input type="submit" class="button" value="<fmt:message key="register"/>"></td>
    </tr>
  </table>
  <br>
</form:form>