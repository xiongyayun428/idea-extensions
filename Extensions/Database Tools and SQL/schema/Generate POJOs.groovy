import com.intellij.database.model.DasTable
import com.intellij.database.util.Case
import com.intellij.database.util.DasUtil

import java.text.SimpleDateFormat

/*
 * Available context bindings:
 *   SELECTION   Iterable<DasObject>
 *   PROJECT     project
 *   FILES       files helper
 */

packageName = "com.sample;"
typeMapping = [
        (~/(?i)smallint|mediumint|int/)          : "Integer",
        (~/(?i)bigint/)                          : "Long",
        (~/(?i)float/)                           : "Float",
        (~/(?i)double/)                          : "Double",
        (~/(?i)bool|bit|tinyint/)                : "Boolean",
        (~/(?i)decimal/)                         : "BigDecimal",
        (~/(?i)date|time|datetime|timestamp/)    : "Date",
        (~/(?i)blob|binary|bfile|clob|raw|image/): "InputStream",
        (~/(?i)/)                                : "String"
]

FILES.chooseDirectoryAndSave("Choose directory", "Choose where to store generated files") { dir ->
    SELECTION.filter { it instanceof DasTable }.each { generate(it, dir) }
}

def generate(table, dir) {
    def className = javaName(table.getName(), true)
    def fields = calcFields(table)
    packageName = getPackageName(dir)
    new File(dir, className + ".java").withPrintWriter { out -> generate(out, className, fields, table) }
}

// 获取包所在文件夹路径
def getPackageName(dir) {
    return dir.toString().replaceAll("\\\\", ".").replaceAll("/", ".").replaceAll("^.*src(\\.main\\.java\\.)?", "") + ";"
}

def generate(out, className, fields, table) {
    out.println "package $packageName"
    out.println ""
    out.println "import com.xyy.athena.core.model.BaseEntity;"
    out.println "import lombok.Data;"
    out.println ""
    out.println "import javax.persistence.Column;"
    out.println "import javax.persistence.Entity;"
    out.println "import javax.persistence.Table;"
    out.println "import javax.persistence.Id;"
    Set types = new HashSet()
    fields.each() {
        types.add(it.type)
    }
    if (types.contains("Date")) {
        out.println "import java.util.Date;"
    }
    if (types.contains("InputStream")) {
        out.println "import java.io.InputStream;"
    }
    out.println ""
    out.println "/**"
    out.println " * $className"
    out.println " *"
    out.println " * @author: XYY"
    out.println " * @date: ${new SimpleDateFormat("yyyy-MM-dd").format(new Date())}"
    out.println " */"
    out.println "@Data"
    out.println "@Entity"
    out.println "@Table(name = \"`" + table.getName() + "`\")"
    out.println "public class $className extends BaseEntity {"
    out.println genSerialID()
    out.println ""
    fields.each() {
        out.println "\t/**"
        out.println "\t * ${it.commoent.toString()}"
        out.println "\t */"
        if ("id" == Case.LOWER.apply(it.name)) out.println "\t@Id"
        if (it.annos != "") out.println "\t${it.annos}"
        out.println "\tprivate ${it.type} ${it.name};"
        out.println ""
    }
    out.println "}"
}

def calcFields(table) {
    DasUtil.getColumns(table).reduce([]) { fields, col ->
        def spec = Case.LOWER.apply(col.getDataType().getSpecification())
        def typeStr = typeMapping.find { p, t -> p.matcher(spec).find() }.value
        fields += [[
                       name    : javaName(col.getName(), false),
                       type    : typeStr,
                       commoent: col.getComment(),
                       annos   : "@Column(name = \"`" + col.getName() + "`\")"
                   ]]
    }
}

def javaName(str, capitalize) {
    def s = com.intellij.psi.codeStyle.NameUtil.splitNameIntoWords(str)
            .collect { Case.LOWER.apply(it).capitalize() }
            .join("")
            .replaceAll(/[^\p{javaJavaIdentifierPart}[_]]/, "_")
    capitalize || s.length() == 1 ? s : Case.LOWER.apply(s[0]) + s[1..-1]
}


static String genSerialID() {
    return "\tprivate static final long serialVersionUID = " + Math.abs(new Random().nextLong()) + "L;"
}