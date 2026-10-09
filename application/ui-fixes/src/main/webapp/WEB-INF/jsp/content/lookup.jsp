<%@ include file="../include/include.jsp" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<h1 class="title"><fmt:message key="customer.lookup"/></h1>

<p><fmt:message key="fill.out.the.following"/></p>

<form:form method="post" action="lookup.htm" modelAttribute="lookupForm">
  <table class="form2" >
    <tr>
      <td align="right" width="30%"><b><label for="firstName"><fmt:message key="first.name"/>:</label></b></td>
      <td width="20%">
        <form:input cssClass="input" path="firstName"/>
      </td>
      <td width="50%">
        <form:errors path="firstName" cssClass="error"/>
      </td>
    </tr>
    <tr>
      <td align="right" width="30%"><b><label for="lastName"><fmt:message key="last.name"/>:</label></b></td>
      <td width="20%">
        <form:input cssClass="input" path="lastName"/>
      </td>
      <td width="50%">
        <form:errors path="lastName" cssClass="error"/>
      </td>
    </tr>
    <tr>
      <td align="right" width="30%"><b><label for="address.street"><fmt:message key="address"/>:</label></b></td>
      <td width="20%">
        <form:input cssClass="input" path="address.street"/>
      </td>
      <td width="50%">
        <form:errors path="address.street" cssClass="error"/>
      </td>
    </tr>
    <tr>
      <td align="right" width="30%"><b><label for="address.city"><fmt:message key="city"/>:</label></b></td>
      <td width="20%">
        <form:input cssClass="input" path="address.city"/>
      </td>
      <td width="50%">
        <form:errors path="address.city" cssClass="error"/>
      </td>
    </tr>
    <tr>
      <td align="right" width="30%"><b><label for="address.state"><fmt:message key="state"/>:</label></b></td>
      <td width="20%">
        <form:input cssClass="input" path="address.state"/>
      </td>
      <td width="50%">
        <form:errors path="address.state" cssClass="error"/>
      </td>
    </tr>
    <tr>
      <td align="right" width="30%"><b><label for="address.zipCode"><fmt:message key="zip.code"/>:</label></b></td>
      <td width="20%">
        <form:input cssClass="input" path="address.zipCode"/>
      </td>
      <td width="50%">
        <form:errors path="address.zipCode" cssClass="error"/>
      </td>
    </tr>
    <tr>
      <td align="right" width="30%"><b><label for="ssn"><fmt:message key="ssn"/>:</label></b></td>
      <td width="20%">
        <form:input cssClass="input" path="ssn"/>
      </td>
      <td width="50%">
        <form:errors path="ssn" cssClass="error"/>
      </td>
    </tr>
    <tr>
      <td>&nbsp;</td>
      <td colspan="2"><input type="submit" class="button" value="<fmt:message key="find.my.login.info"/>"></td>
    </tr>
  </table>
  <br>
 </form:form>