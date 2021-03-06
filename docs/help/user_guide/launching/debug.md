<html>
<head>
<link href="PLUGINS_ROOT/org.robotframework.ide.eclipse.main.plugin.doc.user/help/style.css" rel="stylesheet" type="text/css"/>
</head>
<body>
<a href="RED/../../../../help/index.html">RED - Robot Editor User Guide</a> &gt; <a href="RED/../../../../help/user_guide/user_guide.html">User guide</a> &gt; <a href="RED/../../../../help/user_guide/launching.html">Launching Tests</a> &gt; 
	<h2>Debugging Robot</h2>
<p>Debug functionality is unique way of checking what is happening during tests execution. It works similar 
	as debug functionality in most of programming languages - it allows to track execution of a program	for 
	checking unwanted behavior either on tests side or in tested software.
	</p>
<dl class="warning">
<dt>Warning</dt>
<dd>Avoid making changes in scripts/suites when debugging as you may encounter problems with finding
	   proper code elements to suspend on.
	   </dd>
</dl>
<dl class="note">
<dt>Note</dt>
<dd>Step-into works only for Robot written Keywords. If you wish to step-into Python Keywords, check 
	   <a href="debug/robot_python_debug.html">Debug Robot and Python scripts</a> help content.
	   </dd>
</dl>
<h3>Basics</h3>
<p>In order to work with debugger please save any unsaved files beforehand. Debugging is done inside <b>Debug</b>
	perspective however you don't need to activate it as by default RED will ask if you want to activate it once 
	the tests execution suspends. The same is true for editors: debugger will open an editor with currently executing
	file when suspended.
	</p>
<p>Execution may be suspended due to couple of reasons:
	</p>
<ul>
<li><b>user requested suspend</b> - this is done by pressing <b>Suspend</b> button as described in 
		<a href="exec_control.html">Controlling execution topic</a>,
		<p></p></li>
<li><b>breakpoint hit</b> - whenever <a href="debug/breakpoints.html">breakpoint</a> (a place in code) 
		defined by the user have been hit,
		<p></p></li>
<li><b>erroneous state suspension</b> - debugger may go into erroneous state. This may happen when running tests
		locally (for example when test uses unknown keyword) however it is more probable in remote execution when local
		code does not exactly match remote code. By default RED will ask if execution should suspend but this behavior
		is configurable in <a href="debug/preferences.html">preferences</a>,   
		<p></p></li>
<li><b>end of step</b> - when suspended due to one of reasons above user may ask debugger to perform a step
		(e.g. step over current keyword call), such step will result in another suspension just after current instruction.
		</li>
</ul>
<p>For more information about suspending execution and working with it please refer to <a href="debug/hitting_a_breakpoint.html">
	Suspended execution</a> topic.
	</p>
<h3>Starting debugging session</h3>
<h4>Place breakpoint in RobotFramework executable code</h4>
<p>First thing when working with debugger is to place at least one breakpoint. This allows RED to pause 
	the execution and activate stepping options. You may add breakpoint inside the editor at <b>Source</b> page
	either by double clicking on left-side ruler, choosing <b>Toggle breakpoint</b> option from context menu of this 
	ruler or by hitting <kbd>Ctrl</kbd>+<kbd>Shift</kbd>+<kbd>B</kbd> shortcut (this will add breakpoint for the line
	in which the caret is located). When successful, blue ball icon will appear next to it and new breakpoint entry will be 
	visible in Breakpoint view.
	</p>
<h4>Start Debug</h4>
<p>To start debugging you need to <a href="ui_elements.html">launch</a> the configuration in debug mode. 
	For example by clicking on "green bug" icon at the top toolbar:</p>
<img src="images/debug_3.png"/>
<h4>Limiting test cases to be debugged</h4>
<p>You may edit the launch configuration in order to limit test cases which should be executed in your debug
	session. Open 
	<code><a class="command" href="javascript:executeCommand('org.eclipse.debug.ui.commands.OpenDebugConfigurations')">
	Run -> Debug Configurations...</a></code> dialog and choose which cases should be executed:
	</p>
<img src="images/debug_4.png"/>
<br/>
<h3>Contents</h3>
<ul>
<li><a href="RED/../../../../help/user_guide/launching/debug/ui_elements.html">User interface</a>
</li>
<li><a href="RED/../../../../help/user_guide/launching/debug/breakpoints.html">Breakpoints</a>
</li>
<li><a href="RED/../../../../help/user_guide/launching/debug/hitting_a_breakpoint.html">Hitting a breakpoint</a>
</li>
<li><a href="RED/../../../../help/user_guide/launching/debug/preferences.html">Debugger preferences</a>
</li>
<li><a href="RED/../../../../help/user_guide/launching/debug/robot_python_debug.html">Debugging Robot &amp; Python with RED &amp; PyDev</a>
</li>
</ul>
</body>
</html>