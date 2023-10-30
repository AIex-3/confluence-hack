// 
// Decompiled by Procyon v0.5.36
// 

package com.jsos.shell;

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import javax.servlet.http.HttpSession;
import java.util.Vector;
import java.util.Random;
import java.io.PrintWriter;
import javax.servlet.http.HttpUtils;
import java.io.IOException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.ServletException;
import javax.servlet.ServletConfig;
import java.util.Hashtable;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;

public class ShellServlet extends HttpServlet
{
    private static final String VERSION = "ver. 2.1";
    private static final String CPR = "&copy;&nbsp;<a href=mailto:info@servletsuite.com>Coldbeans</a>&nbsp;";
    private static Object SessionIdLock;
    private static final int HOW_LONG = 6;
    private static final int MAX_WML = 900;
    private static final String DEFBGCOLOR = "#000000";
    private static final String DEFBGCOLOR1 = "#D3D3D3";
    private static final String DEFFGCOLOR = "#008000";
    private static final String DEFFGCOLOR1 = "#000000";
    private static final String DEFFRAMES = "80%,20%";
    private static final String DEFTITLE = "Coldbeans Shell Servlet";
    private static final String DEFPREFIX = "";
    private static final String CONFIG = "config";
    private static final String FICT = "fct";
    private static final String CMD = "cmd";
    private static final String ACTION = "act";
    private static final String UPFRAME = "1";
    private static final String DOWNFRAME = "2";
    private static final String RUNCMD = "3";
    private static final String HISTORY = "4";
    private static final String TITLE = "title";
    private static final String PREFIX = "prefix";
    private static final String BGCOLOR = "bgcolor";
    private static final String BGCOLOR1 = "bgcolor1";
    private static final String FGCOLOR = "fgcolor";
    private static final String FGCOLOR1 = "fgcolor1";
    private static final String FACE = "face";
    private static final String SIZE = "size";
    private static final String FRAMES = "frames";
    private static String NEWLINE;
    private static String separator;
    private static String OS;
    private ServletContext context;
    private Hashtable cnf;
    
    public void init(final ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        this.context = servletConfig.getServletContext();
        ShellServlet.NEWLINE = System.getProperty("line.separator");
        ShellServlet.separator = System.getProperty("file.separator");
        this.cnf = new Hashtable();
        this.readConfig(this.getInitParameter("config"), this.cnf);
    }
    
    public void doPost(final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse) throws ServletException, IOException {
        if (httpServletRequest.getContentLength() > 20480) {
            httpServletResponse.setContentType("text/html");
            final ServletOutputStream outputStream = httpServletResponse.getOutputStream();
            outputStream.println("<html><head><title>Too big</title></head>");
            outputStream.println("<body><h1>Error - content length &gt;20k not ");
            outputStream.println("</h1></body></html>");
        }
        else {
            this.doGet(httpServletRequest, httpServletResponse);
        }
    }
    
    public void doGet(final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse) throws ServletException, IOException {
        String s = HttpUtils.getRequestURL(httpServletRequest).toString();
        final int index;
        if ((index = s.indexOf("?")) > 0) {
            s = s.substring(0, index);
        }
        final String queryString = httpServletRequest.getQueryString();
        if (httpServletRequest.getHeader("Accept").indexOf("wap.wml") >= 0) {
            this.wmlClient(s, queryString, httpServletRequest, httpServletResponse);
            return;
        }
        httpServletResponse.setContentType("text/html");
        final PrintWriter writer = httpServletResponse.getWriter();
        if (queryString == null) {
            writer.println(this.writeFrame(s, httpServletResponse));
        }
        else {
            final String fromQuery = this.getFromQuery(queryString, "act=");
            if (fromQuery.equals("1")) {
                writer.println(this.writeUp());
            }
            else if (fromQuery.equals("2")) {
                writer.println(this.writeDown(s, httpServletRequest, httpServletResponse));
            }
            else if (fromQuery.equals("3")) {
                writer.println(this.writeCommand(httpServletRequest));
            }
            else if (fromQuery.equals("4")) {
                writer.println(this.writeHistory(httpServletRequest));
            }
            else {
                writer.println(this.writeFrame(s, httpServletResponse));
            }
        }
        writer.flush();
        writer.close();
    }
    
    private void wmlClient(final String s, final String s2, final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse) throws IOException {
        httpServletResponse.setContentType("text/vnd.wap.wml");
        final PrintWriter writer = httpServletResponse.getWriter();
        writer.println("<?xml version=\"1.0\"?>");
        writer.println("<!DOCTYPE wml PUBLIC \"-//WAPFORUM//DTD WML 1.1//EN\" \"http://www.wapforum.org/DTD/wml_1.1.xml\">");
        if (s2 == null) {
            writer.println(this.MainWmlCard(s));
        }
        else if (this.getFromQuery(s2, "act=").equals("3")) {
            writer.println(this.writeWmlCommand(s, httpServletRequest));
        }
        else {
            writer.println(this.MainWmlCard(s));
        }
        writer.flush();
        writer.close();
    }
    
    private String MainWmlCard(final String str) {
        final StringBuffer sb = new StringBuffer("");
        sb.append("<wml>\n");
        sb.append("<head>\n");
        sb.append("<meta http-equiv=\"Cache-Control\" content=\"max-age=0\" forua=\"true\"/>\n");
        sb.append("</head>\n");
        sb.append("<card id=\"mainshell\" title=\"Shell Card\">\n");
        sb.append("<do type=\"accept\" label=\"Exec\">\n");
        sb.append("<go href=\"" + str + "?act=3&amp;fct=" + this.getId() + "\" method=\"post\">\n");
        sb.append("<postfield name=\"cmd\" value=\"$(sCmd)\"/>\n");
        sb.append("</go>\n");
        sb.append("</do>\n");
        sb.append("<p align=\"center\"><b>" + this.cnf.get("title") + "</b></p>\n");
        sb.append("<p>Shell&gt;<input type=\"text\" name=\"sCmd\" value=\"\" emptyok=\"false\"/></p>\n");
        sb.append("</card>\n");
        sb.append("</wml>\n");
        return sb.toString();
    }
    
    private String getId() {
        String str = "";
        synchronized (ShellServlet.SessionIdLock) {
            final long currentTimeMillis = System.currentTimeMillis();
            final Random random = new Random();
            str = String.valueOf(currentTimeMillis);
            for (int i = 1; i <= 6; ++i) {
                str += (int)(1.0 + 6.0 * random.nextDouble());
            }
        }
        return str;
    }
    
    private String getFromQuery(final String s, final String str) {
        if (s == null) {
            return "";
        }
        final int index;
        if ((index = s.indexOf(str)) < 0) {
            return "";
        }
        final String substring = s.substring(index + str.length());
        final int index2;
        if ((index2 = substring.indexOf("&")) < 0) {
            return substring;
        }
        return substring.substring(0, index2);
    }
    
    private String writeFrame(final String s, final HttpServletResponse httpServletResponse) {
        final StringBuffer sb = new StringBuffer();
        sb.append("<html>\n");
        sb.append("<head>\n");
        sb.append("<title>" + this.cnf.get("title") + "</title>\n");
        sb.append("</head>\n");
        sb.append("<frameset border=\"0\" rows=\"" + this.cnf.get("frames") + "\">\n");
        sb.append("<frame name=\"up\" src=\"" + httpServletResponse.encodeURL(s + "?" + "act" + "=" + "1") + "\">\n");
        sb.append("<frame name=\"down\" src=\"" + httpServletResponse.encodeURL(s + "?" + "act" + "=" + "2") + "\">\n");
        sb.append("</frameset>\n");
        sb.append("</html>");
        return sb.toString();
    }
    
    private String writeHistory(final HttpServletRequest httpServletRequest) {
        final StringBuffer sb = new StringBuffer("");
        final String font = this.getFont(this.cnf.get("fgcolor1"));
        final Vector vector = (Vector)httpServletRequest.getSession(false).getAttribute("4");
        boolean b = true;
        if (vector == null) {
            b = false;
        }
        else if (vector.size() == 0) {
            b = false;
        }
        sb.append("<html>\n");
        sb.append("<head>\n");
        sb.append("<title>History</title>\n");
        sb.append("</head>\n");
        sb.append("<body bgcolor=\"" + this.cnf.get("bgcolor1") + "\">\n");
        if (b) {
            sb.append("<script language=\"JavaScript\">\n");
            sb.append("function trsf()\n");
            sb.append("{\n");
            sb.append(" if (document.forms[0].hst.selectedIndex>=0)\n");
            sb.append("  opener.document.forms[0].cmd.value=document.forms[0].hst[document.forms[0].hst.selectedIndex].value;\n");
            sb.append("}\n");
            sb.append("</script>\n");
        }
        sb.append(font);
        sb.append("\n<center>\n");
        if (!b) {
            sb.append("<br><br><br>Currently is empty ...\n");
        }
        else {
            sb.append("<form>\n");
            sb.append("<select name=\"hst\" size=\"10\" style=\"width:250\" onChange=\"trsf();\">\n");
            for (int i = 0; i < vector.size(); ++i) {
                final String s = vector.elementAt(i);
                sb.append("<option value=\"");
                sb.append(s);
                sb.append("\">");
                sb.append(s);
                sb.append("</option>\n");
            }
            sb.append("</select>\n");
            sb.append("<br><br><input type=\"button\" value=\"Close\" onClick=\"window.close();\">\n");
            sb.append("</form>\n");
        }
        sb.append("</center>\n");
        sb.append("</font>\n");
        sb.append("</body>\n");
        sb.append("</html>\n");
        return sb.toString();
    }
    
    private String writeUp() {
        final StringBuffer sb = new StringBuffer();
        sb.append("<html>\n");
        sb.append("<body bgcolor=\"" + this.cnf.get("bgcolor") + "\">\n");
        sb.append("</body>\n");
        sb.append("</html>\n");
        return sb.toString();
    }
    
    private String writeDown(final String s, final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse) {
        final HttpSession session = httpServletRequest.getSession(true);
        final StringBuffer sb = new StringBuffer();
        final String font = this.getFont(this.cnf.get("fgcolor1"));
        sb.append("<html>\n");
        sb.append("<body bgcolor=\"" + this.cnf.get("bgcolor1") + "\">\n");
        sb.append(font);
        sb.append(ShellServlet.NEWLINE);
        sb.append("<table><form method=post action=\"" + httpServletResponse.encodeURL(s + "?" + "act" + "=" + "3") + "\" target=up>\n");
        sb.append("<tr><td nowrap><b>" + font + "shell&gt;</b><input type=text name=\"cmd\" size=72></font></td>\n");
        sb.append("<td nowrap>" + font + "<input type=\"Button\" value=\"Enter\" onClick=\"runThis();\"></font></td>\n");
        sb.append("</tr></form></table>\n");
        sb.append("<br>&nbsp;&nbsp;&nbsp;&nbsp;");
        if (session != null) {
            sb.append("<a href=\"javascript:hst();\">History</a>&nbsp;&nbsp;&nbsp;&nbsp;");
        }
        sb.append("" + ShellServlet.NEWLINE);
        sb.append("<script language=\"JavaScript\">\n");
        sb.append(" document.forms[0].cmd.focus();\n");
        sb.append(" function runThis()\n");
        sb.append("  { if (document.forms[0].cmd.value=='')\n");
        sb.append("    { alert(\"Enter some command !\"); document.forms[0].cmd.focus(); return false; }\n");
        sb.append(" document.forms[0].submit(); }\n");
        if (session != null) {
            sb.append(" function hst()\n");
            sb.append("{\n");
            sb.append("window.open('" + httpServletResponse.encodeURL(s + "?" + "act" + "=" + "4") + "','History','width=300,height=310,left=50,top=50,location=no,toolbar=no,resizable=yes,scrollbars=yes');\n");
            sb.append("}\n");
        }
        sb.append("</script>\n");
        sb.append("</font>\n");
        sb.append("</body>\n");
        sb.append("</html>\n");
        return sb.toString();
    }
    
    private String writeCommand(final HttpServletRequest httpServletRequest) throws IOException {
        final StringBuffer sb = new StringBuffer();
        final String parameter = httpServletRequest.getParameter("cmd");
        final String font = this.getFont(this.cnf.get("fgcolor"));
        final HttpSession session = httpServletRequest.getSession(false);
        sb.append("<html>\n");
        sb.append("<body bgcolor=\"" + this.cnf.get("bgcolor") + "\">\n");
        sb.append("<script language=\"JavaScript\">\n");
        sb.append(" parent.frames[1].document.forms[0].cmd.value='';\n");
        sb.append(" parent.frames[1].document.forms[0].cmd.focus();\n");
        sb.append("</script>\n");
        sb.append(font + "<b>\n");
        sb.append("<br>");
        if (parameter == null) {
            sb.append("</i>wrong command</i>");
        }
        else {
            final String trim = parameter.trim();
            if (trim.length() == 0) {
                sb.append("</i>empty command</i>");
            }
            else {
                if (session != null) {
                    Vector<String> vector = (Vector<String>)session.getAttribute("4");
                    if (vector == null) {
                        vector = new Vector<String>();
                    }
                    if (vector.size() == 0) {
                        vector.addElement(trim);
                    }
                    else {
                        vector.insertElementAt(trim, 0);
                    }
                    session.setAttribute("4", (Object)vector);
                }
                sb.append(this.executeCmd(this.cnf.get("prefix") + trim, "<br>"));
            }
        }
        sb.append("</b></font>");
        sb.append("</body>\n");
        sb.append("</html>\n");
        return sb.toString();
    }
    
    private String writeWmlCommand(final String str, final HttpServletRequest httpServletRequest) throws IOException {
        final StringBuffer sb = new StringBuffer();
        String parameter = httpServletRequest.getParameter("cmd");
        if (parameter == null) {
            parameter = "";
        }
        sb.append("<wml>\n");
        sb.append("<head>\n");
        sb.append("<meta http-equiv=\"Cache-Control\" content=\"max-age=0\" forua=\"true\"/>\n");
        sb.append("</head>\n");
        sb.append("<card id=\"execshell\" title=\"Shell Card\">\n");
        sb.append("<do type=\"accept\" label=\"Ok\">\n");
        sb.append("<go href=\"" + str + "?fct=" + this.getId() + "\"/>\n");
        sb.append("</do>\n");
        final String trim = parameter.trim();
        if (trim.length() == 0) {
            sb.append("<p>empty command</p>");
        }
        else {
            String s = this.executeCmd(this.cnf.get("prefix") + trim, "<br/>");
            if (s.length() > 900) {
                s = s.substring(0, 900) + "...";
            }
            sb.append("<p>");
            sb.append(this.replaceDollar(s));
            sb.append("</p>");
        }
        sb.append("</card>\n");
        sb.append("</wml>\n");
        return sb.toString();
    }
    
    public static String getOsName() {
        if (ShellServlet.OS == null) {
            ShellServlet.OS = System.getProperty("os.name");
        }
        return ShellServlet.OS;
    }
    
    public static boolean isWindows() {
        return getOsName().startsWith("Windows");
    }
    
    private String executeCmd(final String str, final String s) {
        final StringBuffer sb = new StringBuffer(s + "shell&gt;" + str);
        try {
            if (isWindows()) {
                final Process start = new ProcessBuilder(new String[] { "cmd", "/c", str }).start();
                final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(start.getInputStream()));
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    sb.append(s);
                    sb.append(this.prepareMsg(line));
                    sb.append(ShellServlet.NEWLINE);
                }
                start.waitFor();
                bufferedReader.close();
            }
            else {
                final Process start2 = new ProcessBuilder(new String[] { "sh", "-c", str }).start();
                final BufferedReader bufferedReader2 = new BufferedReader(new InputStreamReader(start2.getInputStream()));
                String line2;
                while ((line2 = bufferedReader2.readLine()) != null) {
                    sb.append(s);
                    sb.append(this.prepareMsg(line2));
                    sb.append(ShellServlet.NEWLINE);
                }
                start2.waitFor();
                bufferedReader2.close();
            }
        }
        catch (Exception obj) {
            sb.append(s);
            sb.append(obj);
        }
        return sb.toString();
    }
    
    public String getServletInfo() {
        return "A servlet that supports shell command execution ver. 2.1";
    }
    
    private String prepareMsg(final String s) {
        final StringBuffer sb = new StringBuffer("");
        if (s.length() == 0) {
            return "";
        }
        for (int i = 0; i < s.length(); ++i) {
            final char char1 = s.charAt(i);
            if (char1 == '>') {
                sb.append("&gt;");
            }
            else if (char1 == '<') {
                sb.append("&lt;");
            }
            else if (char1 == '&') {
                sb.append("&amp;");
            }
            else if (char1 == '\"') {
                sb.append("&quot;");
            }
            else {
                sb.append(char1);
            }
        }
        return sb.toString();
    }
    
    private String replaceDollar(final String s) {
        final StringBuffer sb = new StringBuffer("");
        for (int i = 0; i < s.length(); ++i) {
            final char char1;
            if ((char1 = s.charAt(i)) == '$') {
                sb.append("$$");
            }
            else {
                sb.append(char1);
            }
        }
        return sb.toString();
    }
    
    private String getFont(final String str) {
        String str2 = "<font color=\"" + str + "\"";
        final String str3;
        if ((str3 = this.cnf.get("face")) != null) {
            str2 = str2 + " face=\"" + str3 + "\"";
        }
        final String str4;
        if ((str4 = this.cnf.get("size")) != null) {
            str2 = str2 + " size=\"" + str4 + "\"";
        }
        return str2 + ">";
    }
    
    private void readConfig(final String s, final Hashtable hashtable) {
        try {
            String line;
            while ((line = new BufferedReader(new InputStreamReader(new FileInputStream(this.lookupFile(s)))).readLine()) != null) {
                final String trim = line.trim();
                if (trim.length() > 0) {
                    final int index = trim.indexOf("=");
                    if (index <= 0 || index >= trim.length() - 1 || trim.charAt(0) == '#' || trim.startsWith("//")) {
                        continue;
                    }
                    hashtable.put(trim.substring(0, index).trim(), trim.substring(index + 1).trim());
                }
            }
        }
        catch (Exception ex) {}
        if (hashtable.get("bgcolor") == null) {
            hashtable.put("bgcolor", "#000000");
        }
        if (hashtable.get("bgcolor1") == null) {
            hashtable.put("bgcolor1", "#D3D3D3");
        }
        if (hashtable.get("fgcolor") == null) {
            hashtable.put("fgcolor", "#008000");
        }
        if (hashtable.get("fgcolor1") == null) {
            hashtable.put("fgcolor1", "#000000");
        }
        if (hashtable.get("title") == null) {
            hashtable.put("title", "Coldbeans Shell Servlet");
        }
        if (hashtable.get("frames") == null) {
            hashtable.put("frames", "80%,20%");
        }
        final String str;
        if ((str = hashtable.get("prefix")) == null) {
            hashtable.put("prefix", "");
        }
        else {
            final String string = str + " ";
            hashtable.remove("prefix");
            hashtable.put("prefix", string);
        }
    }
    
    private File lookupFile(final String s) {
        final File file = new File(s);
        return file.isAbsolute() ? file : new File(this.context.getRealPath("/"), s);
    }
    
    static {
        ShellServlet.SessionIdLock = new Object();
        ShellServlet.NEWLINE = "\n";
        ShellServlet.separator = "/";
        ShellServlet.OS = null;
    }
}
