package org.firstinspires.ftc.teamcode.opmodes.mechanismtests;

import static com.arcrobotics.ftclib.gamepad.GamepadKeys.Button.X;
import static com.arcrobotics.ftclib.gamepad.GamepadKeys.Trigger.LEFT_TRIGGER;
import static com.arcrobotics.ftclib.gamepad.GamepadKeys.Trigger.RIGHT_TRIGGER;
import static org.firstinspires.ftc.teamcode.subsystems.Intake.ANGLE_EXTENDO_EXTENDED_MAX;
import static org.firstinspires.ftc.teamcode.subsystems.Intake.ANGLE_EXTENDO_RETRACTED;
import static org.firstinspires.ftc.teamcode.subsystems.Intake.SPEED_MULTIPLIER_EXTENDO;
import static org.firstinspires.ftc.teamcode.subsystems.utilities.SimpleServoPivot.getGoBildaServo;
import static org.firstinspires.ftc.teamcode.subsystems.utilities.SimpleServoPivot.getReversedServo;

import com.arcrobotics.ftclib.gamepad.GamepadEx;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

import org.firstinspires.ftc.teamcode.subsystems.utilities.SimpleServoPivot;

public final class TestExtendo extends LinearOpMode {

    @Override
    public void runOpMode() throws InterruptedException {

        double extendedAngle = ANGLE_EXTENDO_EXTENDED_MAX;

        SimpleServoPivot extendo = new SimpleServoPivot(
                ANGLE_EXTENDO_RETRACTED,
                extendedAngle,
                getReversedServo(getGoBildaServo(hardwareMap, "extendo right")),
                getGoBildaServo(hardwareMap, "extendo left")
        );

        GamepadEx gamepadEx1 = new GamepadEx(gamepad1);

        waitForStart();

        while (opModeIsActive()) {

            gamepadEx1.readButtons();

            if (gamepadEx1.wasJustPressed(X)) extendo.toggle();

            extendedAngle += SPEED_MULTIPLIER_EXTENDO * (gamepadEx1.getTrigger(RIGHT_TRIGGER) - gamepadEx1.getTrigger(LEFT_TRIGGER));

            extendo.updateAngles(ANGLE_EXTENDO_RETRACTED,extendedAngle);
            extendo.run();

        }

    }

}
