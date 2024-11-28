package org.firstinspires.ftc.teamcode.subsystem;

import static org.firstinspires.ftc.teamcode.opmode.OpModeVars.divider;

import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.roadrunner.Pose2d;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.roadrunner.MecanumDrive;
import org.firstinspires.ftc.teamcode.subsystem.utility.BulkReader;

@Config
public final class Robot {

    public final MecanumDrive drivetrain;
    public final Intake intake;
    public final Deposit deposit;
    public final Climber climber;
    public final Lift lift;

    private final BulkReader bulkReader;

    public Robot(HardwareMap hardwareMap, Pose2d startPose) {

        drivetrain = new MecanumDrive(hardwareMap, startPose);
        bulkReader = new BulkReader(hardwareMap);
        intake = new Intake(hardwareMap);
        lift = new Lift(hardwareMap);
        deposit = new Deposit(hardwareMap, lift);
        climber = new Climber(hardwareMap, lift);
    }

    public void preload(boolean backdropSide) {
    }

    public void initRun() {
    }

    public void endgame() {
    }

    public void readSensors() {
        bulkReader.bulkRead();
        drivetrain.updatePoseEstimate();
    }

    public void run() {

        if (intake.awaitingTransfer()) deposit.transfer(intake.transfer());

        intake.run(deposit.hasSample(), deposit.isActive());
        deposit.run(intake.clearOfDeposit(), climber.isActive());
        climber.run();

        lift.run(intake.clearOfDeposit(), climber.isActive());
    }

    public boolean requestingSlowMode() {
        return false;
//                deposit.movingToScore() && intake.clearOfDeposit(); // deposit intends to move and intake is not blocking it
    }

    public void printTelemetry() {
        drivetrain.printTelemetry();
        divider();
        intake.printTelemetry();
        divider();
        deposit.printTelemetry();
        divider();
        climber.printTelemetry();
        divider();
        lift.printTelemetry();
    }
}
