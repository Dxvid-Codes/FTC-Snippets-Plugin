/** FtcFileTemplates.kt */

package ontalent.ftcsnippets.templates

import ontalent.ftcsnippets.settings.FtcFramework
import ontalent.ftcsnippets.settings.FtcLanguage
import ontalent.ftcsnippets.settings.SubsystemPattern

/**
 * Renders complete FTC class files as plain text.
 *
 * Everything here is pure string building on purpose: generating text instead of
 * building PSI means Kotlin files can be produced without the plugin depending on
 * the Kotlin plugin's PSI API.
 */
object FtcFileTemplates {

    private const val SOLVERS = "com.seattlesolvers.solverslib"

    fun render(
        kind: FtcTemplateKind,
        className: String,
        packageName: String,
        language: FtcLanguage,
        framework: FtcFramework,
        pattern: SubsystemPattern
    ): String = when (kind) {
        FtcTemplateKind.AUTONOMOUS -> autonomous(className, packageName, language, framework)
        FtcTemplateKind.TELEOP -> teleOp(className, packageName, language, framework)
        FtcTemplateKind.SUBSYSTEM -> subsystem(className, packageName, language, framework, pattern)
        FtcTemplateKind.COMMAND -> command(className, packageName, language, framework)
        FtcTemplateKind.ROBOT_CONTAINER -> robotContainer(className, packageName, language, framework)
    }

    // ---------------------------------------------------------------- assembly

    private fun compose(
        language: FtcLanguage,
        packageName: String,
        imports: List<String>,
        body: String
    ): String {
        val semi = if (language == FtcLanguage.JAVA) ";" else ""
        val builder = StringBuilder()

        if (packageName.isNotBlank()) {
            builder.append("package ").append(packageName).append(semi).append("\n\n")
        }
        if (imports.isNotEmpty()) {
            imports.forEach { builder.append("import ").append(it).append(semi).append("\n") }
            builder.append("\n")
        }
        builder.append(body.trimEnd()).append("\n")
        return builder.toString()
    }

    // -------------------------------------------------------------- autonomous

    private fun autonomous(
        className: String,
        packageName: String,
        language: FtcLanguage,
        framework: FtcFramework
    ): String = when {
        framework == FtcFramework.SDK && language == FtcLanguage.JAVA -> compose(
            language, packageName,
            listOf(
                "com.qualcomm.robotcore.eventloop.opmode.Autonomous",
                "com.qualcomm.robotcore.eventloop.opmode.LinearOpMode",
                "com.qualcomm.robotcore.util.ElapsedTime"
            ),
            """
            @Autonomous(name = "$className", group = "Autonomous")
            public class $className extends LinearOpMode {

                private final ElapsedTime runtime = new ElapsedTime();

                @Override
                public void runOpMode() throws InterruptedException {
                    // TODO: pull your hardware out of the hardwareMap here, for example
                    // DcMotor leftDrive = hardwareMap.get(DcMotor.class, "leftDrive");

                    telemetry.addLine("Initialized - waiting for start");
                    telemetry.update();

                    waitForStart();
                    runtime.reset();

                    if (isStopRequested()) {
                        return;
                    }

                    // TODO: your autonomous routine goes here

                    telemetry.addData("Runtime", "%.2f s", runtime.seconds());
                    telemetry.update();
                }
            }
            """.trimIndent()
        )

        framework == FtcFramework.SDK && language == FtcLanguage.KOTLIN -> compose(
            language, packageName,
            listOf(
                "com.qualcomm.robotcore.eventloop.opmode.Autonomous",
                "com.qualcomm.robotcore.eventloop.opmode.LinearOpMode",
                "com.qualcomm.robotcore.util.ElapsedTime"
            ),
            """
            @Autonomous(name = "$className", group = "Autonomous")
            class $className : LinearOpMode() {

                private val runtime = ElapsedTime()

                override fun runOpMode() {
                    // TODO: pull your hardware out of the hardwareMap here, for example
                    // val leftDrive = hardwareMap.get(DcMotor::class.java, "leftDrive")

                    telemetry.addLine("Initialized - waiting for start")
                    telemetry.update()

                    waitForStart()
                    runtime.reset()

                    if (isStopRequested) {
                        return
                    }

                    // TODO: your autonomous routine goes here

                    telemetry.addData("Runtime", "%.2f s", runtime.seconds())
                    telemetry.update()
                }
            }
            """.trimIndent()
        )

        language == FtcLanguage.JAVA -> compose(
            language, packageName,
            listOf(
                "com.qualcomm.robotcore.eventloop.opmode.Autonomous",
                "$SOLVERS.command.CommandOpMode",
                "$SOLVERS.command.InstantCommand",
                "$SOLVERS.command.SequentialCommandGroup",
                "$SOLVERS.command.WaitCommand"
            ),
            """
            @Autonomous(name = "$className", group = "Autonomous")
            public class $className extends CommandOpMode {

                @Override
                public void initialize() {
                    // TODO: construct your subsystems here, for example
                    // DriveSubsystem drive = new DriveSubsystem(hardwareMap);

                    schedule(new SequentialCommandGroup(
                            new InstantCommand(() -> telemetry.addLine("Auto running")),
                            new WaitCommand(500)
                            // TODO: chain the rest of your routine
                    ));
                }
            }
            """.trimIndent()
        )

        else -> compose(
            language, packageName,
            listOf(
                "com.qualcomm.robotcore.eventloop.opmode.Autonomous",
                "$SOLVERS.command.CommandOpMode",
                "$SOLVERS.command.InstantCommand",
                "$SOLVERS.command.SequentialCommandGroup",
                "$SOLVERS.command.WaitCommand"
            ),
            """
            @Autonomous(name = "$className", group = "Autonomous")
            class $className : CommandOpMode() {

                override fun initialize() {
                    // TODO: construct your subsystems here, for example
                    // val drive = DriveSubsystem(hardwareMap)

                    schedule(
                        SequentialCommandGroup(
                            InstantCommand({ telemetry.addLine("Auto running") }),
                            WaitCommand(500)
                            // TODO: chain the rest of your routine
                        )
                    )
                }
            }
            """.trimIndent()
        )
    }

    // ------------------------------------------------------------------ teleop

    private fun teleOp(
        className: String,
        packageName: String,
        language: FtcLanguage,
        framework: FtcFramework
    ): String = when {
        framework == FtcFramework.SDK && language == FtcLanguage.JAVA -> compose(
            language, packageName,
            listOf(
                "com.qualcomm.robotcore.eventloop.opmode.LinearOpMode",
                "com.qualcomm.robotcore.eventloop.opmode.TeleOp"
            ),
            """
            @TeleOp(name = "$className", group = "TeleOp")
            public class $className extends LinearOpMode {

                @Override
                public void runOpMode() throws InterruptedException {
                    // TODO: pull your hardware out of the hardwareMap here

                    telemetry.addLine("Initialized - waiting for start");
                    telemetry.update();

                    waitForStart();

                    while (opModeIsActive()) {
                        double drive = -gamepad1.left_stick_y;
                        double strafe = gamepad1.left_stick_x;
                        double turn = gamepad1.right_stick_x;

                        // TODO: feed drive/strafe/turn into your drivetrain

                        telemetry.addData("drive", "%.2f", drive);
                        telemetry.addData("strafe", "%.2f", strafe);
                        telemetry.addData("turn", "%.2f", turn);
                        telemetry.update();
                    }
                }
            }
            """.trimIndent()
        )

        framework == FtcFramework.SDK && language == FtcLanguage.KOTLIN -> compose(
            language, packageName,
            listOf(
                "com.qualcomm.robotcore.eventloop.opmode.LinearOpMode",
                "com.qualcomm.robotcore.eventloop.opmode.TeleOp"
            ),
            """
            @TeleOp(name = "$className", group = "TeleOp")
            class $className : LinearOpMode() {

                override fun runOpMode() {
                    // TODO: pull your hardware out of the hardwareMap here

                    telemetry.addLine("Initialized - waiting for start")
                    telemetry.update()

                    waitForStart()

                    while (opModeIsActive()) {
                        val drive = -gamepad1.left_stick_y.toDouble()
                        val strafe = gamepad1.left_stick_x.toDouble()
                        val turn = gamepad1.right_stick_x.toDouble()

                        // TODO: feed drive/strafe/turn into your drivetrain

                        telemetry.addData("drive", "%.2f", drive)
                        telemetry.addData("strafe", "%.2f", strafe)
                        telemetry.addData("turn", "%.2f", turn)
                        telemetry.update()
                    }
                }
            }
            """.trimIndent()
        )

        language == FtcLanguage.JAVA -> compose(
            language, packageName,
            listOf(
                "com.qualcomm.robotcore.eventloop.opmode.TeleOp",
                "$SOLVERS.command.CommandOpMode",
                "$SOLVERS.command.InstantCommand",
                "$SOLVERS.gamepad.GamepadEx",
                "$SOLVERS.gamepad.GamepadKeys"
            ),
            """
            @TeleOp(name = "$className", group = "TeleOp")
            public class $className extends CommandOpMode {

                private GamepadEx driver;
                private GamepadEx operator;

                @Override
                public void initialize() {
                    driver = new GamepadEx(gamepad1);
                    operator = new GamepadEx(gamepad2);

                    // TODO: construct your subsystems here, for example
                    // DriveSubsystem drive = new DriveSubsystem(hardwareMap);

                    // TODO: bind your buttons, for example
                    // operator.getGamepadButton(GamepadKeys.Button.A)
                    //         .whenPressed(new InstantCommand(() -> telemetry.addLine("A")));
                }
            }
            """.trimIndent()
        )

        else -> compose(
            language, packageName,
            listOf(
                "com.qualcomm.robotcore.eventloop.opmode.TeleOp",
                "$SOLVERS.command.CommandOpMode",
                "$SOLVERS.command.InstantCommand",
                "$SOLVERS.gamepad.GamepadEx",
                "$SOLVERS.gamepad.GamepadKeys"
            ),
            """
            @TeleOp(name = "$className", group = "TeleOp")
            class $className : CommandOpMode() {

                private lateinit var driver: GamepadEx
                private lateinit var operator: GamepadEx

                override fun initialize() {
                    driver = GamepadEx(gamepad1)
                    operator = GamepadEx(gamepad2)

                    // TODO: construct your subsystems here, for example
                    // val drive = DriveSubsystem(hardwareMap)

                    // TODO: bind your buttons, for example
                    // operator.getGamepadButton(GamepadKeys.Button.A)
                    //     .whenPressed(InstantCommand({ telemetry.addLine("A") }))
                }
            }
            """.trimIndent()
        )
    }

    // --------------------------------------------------------------- subsystem

    private fun subsystem(
        className: String,
        packageName: String,
        language: FtcLanguage,
        framework: FtcFramework,
        pattern: SubsystemPattern
    ): String {
        val structured = pattern == SubsystemPattern.READ_LOOP_WRITE
        val solvers = framework == FtcFramework.SOLVERSLIB

        val imports = buildList {
            add("com.qualcomm.robotcore.hardware.DcMotor")
            add("com.qualcomm.robotcore.hardware.HardwareMap")
            if (solvers) add("$SOLVERS.command.SubsystemBase")
        }

        val body = when {
            language == FtcLanguage.JAVA && !structured -> """
            public class $className${if (solvers) " extends SubsystemBase" else ""} {

                private final DcMotor motor;

                public $className(HardwareMap hardwareMap) {
                    motor = hardwareMap.get(DcMotor.class, "motor");
                    motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
                }

                public void setPower(double power) {
                    motor.setPower(power);
                }

                public int getPosition() {
                    return motor.getCurrentPosition();
                }${if (solvers) """

                @Override
                public void periodic() {
                    // Called once per scheduler run.
                }""" else ""}
            }
            """.trimIndent()

            language == FtcLanguage.JAVA -> """
            public class $className${if (solvers) " extends SubsystemBase" else ""} {

                private final DcMotor motor;

                /** Sensor state, refreshed once per loop by {@link #read()}. */
                private int position;

                /** Desired output, pushed to the hardware once per loop by {@link #write()}. */
                private double targetPower;

                public $className(HardwareMap hardwareMap) {
                    motor = hardwareMap.get(DcMotor.class, "motor");
                    motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
                }

                /** Read every sensor exactly once so the rest of the loop is free of I/O. */
                public void read() {
                    position = motor.getCurrentPosition();
                }

                /** Pure logic - never touch hardware from here. */
                public void loop() {
                    // TODO: compute targetPower from the cached state
                }

                /** Push the computed outputs to the hardware. */
                public void write() {
                    motor.setPower(targetPower);
                }

                public void setTargetPower(double targetPower) {
                    this.targetPower = targetPower;
                }

                public int getPosition() {
                    return position;
                }${if (solvers) """

                @Override
                public void periodic() {
                    read();
                    loop();
                    write();
                }""" else ""}
            }
            """.trimIndent()

            !structured -> """
            class $className(hardwareMap: HardwareMap)${if (solvers) " : SubsystemBase()" else ""} {

                private val motor: DcMotor = hardwareMap.get(DcMotor::class.java, "motor").apply {
                    zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
                }

                val position: Int
                    get() = motor.currentPosition

                fun setPower(power: Double) {
                    motor.power = power
                }${if (solvers) """

                override fun periodic() {
                    // Called once per scheduler run.
                }""" else ""}
            }
            """.trimIndent()

            else -> """
            class $className(hardwareMap: HardwareMap)${if (solvers) " : SubsystemBase()" else ""} {

                private val motor: DcMotor = hardwareMap.get(DcMotor::class.java, "motor").apply {
                    zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
                }

                /** Sensor state, refreshed once per loop by [read]. */
                var position: Int = 0
                    private set

                /** Desired output, pushed to the hardware once per loop by [write]. */
                var targetPower: Double = 0.0

                /** Read every sensor exactly once so the rest of the loop is free of I/O. */
                fun read() {
                    position = motor.currentPosition
                }

                /** Pure logic - never touch hardware from here. */
                fun loop() {
                    // TODO: compute targetPower from the cached state
                }

                /** Push the computed outputs to the hardware. */
                fun write() {
                    motor.power = targetPower
                }${if (solvers) """

                override fun periodic() {
                    read()
                    loop()
                    write()
                }""" else ""}
            }
            """.trimIndent()
        }

        return compose(language, packageName, imports, body)
    }

    // ----------------------------------------------------------------- command

    private fun command(
        className: String,
        packageName: String,
        language: FtcLanguage,
        framework: FtcFramework
    ): String = when {
        framework == FtcFramework.SOLVERSLIB && language == FtcLanguage.JAVA -> compose(
            language, packageName,
            listOf("$SOLVERS.command.CommandBase"),
            """
            public class $className extends CommandBase {

                public $className() {
                    // TODO: take your subsystem as a parameter and declare it here
                    // addRequirements(subsystem);
                }

                @Override
                public void initialize() {
                    // Runs once when the command is scheduled.
                }

                @Override
                public void execute() {
                    // Runs every scheduler cycle while the command is active.
                }

                @Override
                public boolean isFinished() {
                    // TODO: return true once the command is done.
                    return false;
                }

                @Override
                public void end(boolean interrupted) {
                    // Always runs last - stop your hardware here.
                }
            }
            """.trimIndent()
        )

        framework == FtcFramework.SOLVERSLIB -> compose(
            language, packageName,
            listOf("$SOLVERS.command.CommandBase"),
            """
            class $className : CommandBase() {

                init {
                    // TODO: take your subsystem as a constructor parameter and declare it here
                    // addRequirements(subsystem)
                }

                override fun initialize() {
                    // Runs once when the command is scheduled.
                }

                override fun execute() {
                    // Runs every scheduler cycle while the command is active.
                }

                override fun isFinished(): Boolean {
                    // TODO: return true once the command is done.
                    return false
                }

                override fun end(interrupted: Boolean) {
                    // Always runs last - stop your hardware here.
                }
            }
            """.trimIndent()
        )

        // Plain SDK has no scheduler, so generate a self-contained state machine
        // step that an OpMode can drive by hand.
        language == FtcLanguage.JAVA -> compose(
            language, packageName, emptyList(),
            """
            /**
             * A hand-rolled command. The plain FTC SDK has no command scheduler, so drive
             * this from your OpMode loop:
             *
             * <pre>
             * $className command = new $className();
             * command.initialize();
             * while (opModeIsActive() && !command.isFinished()) {
             *     command.execute();
             * }
             * command.end(false);
             * </pre>
             */
            public class $className {

                public void initialize() {
                    // Runs once before the first execute().
                }

                public void execute() {
                    // Runs every loop while the command is active.
                }

                public boolean isFinished() {
                    // TODO: return true once the command is done.
                    return true;
                }

                public void end(boolean interrupted) {
                    // Always runs last - stop your hardware here.
                }
            }
            """.trimIndent()
        )

        else -> compose(
            language, packageName, emptyList(),
            """
            /**
             * A hand-rolled command. The plain FTC SDK has no command scheduler, so drive
             * this from your OpMode loop:
             *
             * ```
             * val command = $className()
             * command.initialize()
             * while (opModeIsActive() && !command.isFinished()) {
             *     command.execute()
             * }
             * command.end(false)
             * ```
             */
            class $className {

                fun initialize() {
                    // Runs once before the first execute().
                }

                fun execute() {
                    // Runs every loop while the command is active.
                }

                fun isFinished(): Boolean {
                    // TODO: return true once the command is done.
                    return true
                }

                fun end(interrupted: Boolean) {
                    // Always runs last - stop your hardware here.
                }
            }
            """.trimIndent()
        )
    }

    // --------------------------------------------------------- robot container

    private fun robotContainer(
        className: String,
        packageName: String,
        language: FtcLanguage,
        framework: FtcFramework
    ): String = when {
        framework == FtcFramework.SDK && language == FtcLanguage.JAVA -> compose(
            language, packageName,
            listOf(
                "com.qualcomm.robotcore.hardware.HardwareMap",
                "org.firstinspires.ftc.robotcore.external.Telemetry"
            ),
            """
            /**
             * Owns every subsystem on the robot so OpModes stay short. Call
             * {@link #read()}, {@link #loop()} and {@link #write()} once per OpMode loop.
             */
            public class $className {

                private final HardwareMap hardwareMap;
                private final Telemetry telemetry;

                // TODO: declare your subsystems here
                // public final DriveSubsystem drive;

                public $className(HardwareMap hardwareMap, Telemetry telemetry) {
                    this.hardwareMap = hardwareMap;
                    this.telemetry = telemetry;

                    // TODO: construct your subsystems here
                    // drive = new DriveSubsystem(hardwareMap);
                }

                /** Refresh cached sensor state for every subsystem. */
                public void read() {
                    // drive.read();
                }

                /** Run the logic for every subsystem. */
                public void loop() {
                    // drive.loop();
                }

                /** Push every subsystem's outputs to the hardware. */
                public void write() {
                    // drive.write();
                    telemetry.update();
                }
            }
            """.trimIndent()
        )

        framework == FtcFramework.SDK -> compose(
            language, packageName,
            listOf(
                "com.qualcomm.robotcore.hardware.HardwareMap",
                "org.firstinspires.ftc.robotcore.external.Telemetry"
            ),
            """
            /**
             * Owns every subsystem on the robot so OpModes stay short. Call
             * [read], [loop] and [write] once per OpMode loop.
             */
            class $className(
                private val hardwareMap: HardwareMap,
                private val telemetry: Telemetry
            ) {

                // TODO: construct your subsystems here
                // val drive = DriveSubsystem(hardwareMap)

                /** Refresh cached sensor state for every subsystem. */
                fun read() {
                    // drive.read()
                }

                /** Run the logic for every subsystem. */
                fun loop() {
                    // drive.loop()
                }

                /** Push every subsystem's outputs to the hardware. */
                fun write() {
                    // drive.write()
                    telemetry.update()
                }
            }
            """.trimIndent()
        )

        language == FtcLanguage.JAVA -> compose(
            language, packageName,
            listOf(
                "com.qualcomm.robotcore.hardware.Gamepad",
                "com.qualcomm.robotcore.hardware.HardwareMap",
                "$SOLVERS.command.Command",
                "$SOLVERS.command.InstantCommand",
                "$SOLVERS.gamepad.GamepadEx",
                "$SOLVERS.gamepad.GamepadKeys"
            ),
            """
            /**
             * Owns every subsystem and every button binding, so the OpModes only have to
             * construct this class and schedule {@link #getAutonomousCommand()}.
             */
            public class $className {

                private final GamepadEx driver;
                private final GamepadEx operator;

                // TODO: declare your subsystems here
                // public final DriveSubsystem drive;

                public $className(HardwareMap hardwareMap, Gamepad gamepad1, Gamepad gamepad2) {
                    driver = new GamepadEx(gamepad1);
                    operator = new GamepadEx(gamepad2);

                    // TODO: construct your subsystems here
                    // drive = new DriveSubsystem(hardwareMap);

                    configureBindings();
                }

                private void configureBindings() {
                    // TODO: bind your buttons, for example
                    // operator.getGamepadButton(GamepadKeys.Button.A)
                    //         .whenPressed(new InstantCommand(() -> {}));
                }

                /** The routine an Autonomous OpMode should schedule. */
                public Command getAutonomousCommand() {
                    return new InstantCommand(() -> {});
                }
            }
            """.trimIndent()
        )

        else -> compose(
            language, packageName,
            listOf(
                "com.qualcomm.robotcore.hardware.Gamepad",
                "com.qualcomm.robotcore.hardware.HardwareMap",
                "$SOLVERS.command.Command",
                "$SOLVERS.command.InstantCommand",
                "$SOLVERS.gamepad.GamepadEx",
                "$SOLVERS.gamepad.GamepadKeys"
            ),
            """
            /**
             * Owns every subsystem and every button binding, so the OpModes only have to
             * construct this class and schedule [getAutonomousCommand].
             */
            class $className(hardwareMap: HardwareMap, gamepad1: Gamepad, gamepad2: Gamepad) {

                private val driver = GamepadEx(gamepad1)
                private val operator = GamepadEx(gamepad2)

                // TODO: construct your subsystems here
                // val drive = DriveSubsystem(hardwareMap)

                init {
                    configureBindings()
                }

                private fun configureBindings() {
                    // TODO: bind your buttons, for example
                    // operator.getGamepadButton(GamepadKeys.Button.A)
                    //     .whenPressed(InstantCommand({}))
                }

                /** The routine an Autonomous OpMode should schedule. */
                fun getAutonomousCommand(): Command = InstantCommand({})
            }
            """.trimIndent()
        )
    }
}
