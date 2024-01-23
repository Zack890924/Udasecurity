package com.udacity.catpoint.security.service;


import com.udacity.catpoint.image.service.FakeImageService;
import com.udacity.catpoint.security.application.StatusListener;
import com.udacity.catpoint.security.data.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import org.junit.jupiter.api.function.Executable;
import java.util.*;
import java.util.stream.IntStream;

import com.udacity.catpoint.image.service.ImageService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {
    private SecurityService securityService;
    private Sensor sensor;
    @Mock
    private ImageService imageService;
    @Mock
    private SecurityRepository securityRepository;
    @Mock
    private StatusListener statusListener;


    @BeforeEach
    void setup(){

        securityService = new SecurityService(securityRepository, imageService);
        sensor = new Sensor(UUID.randomUUID().toString(), SensorType.DOOR);

    }
    @org.jetbrains.annotations.NotNull
    private Set<Sensor> getSensors(boolean active, int number){
        String randomString = UUID.randomUUID().toString();
        Set<Sensor> sensors = new HashSet<>();
        IntStream.range(0, number)
                .forEach(i -> {
                    Sensor sensor = new Sensor(randomString + "_" + i, SensorType.DOOR);
                    sensor.setActive(active);
                    sensors.add(sensor);
                });
        return sensors;
    }
    //Test1
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void SystemArmedAndSensorInactivated_ChangeToPending(ArmingStatus armingStatus){
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        when(securityRepository.getArmingStatus()).thenReturn(armingStatus);
        securityService.changeSensorActivationStatus(sensor,true);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
   }
   //Test2
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void SystemArmedAndSensorInactivated_PendingChangeToAlarm(ArmingStatus armingStatus){
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(securityRepository.getArmingStatus()).thenReturn(armingStatus);
        securityService.changeSensorActivationStatus(sensor,true);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
   }
   //Test3
   @Test
   void SensorInActiveAndPendingAlarm_ChangeToNoAlarm() {
       Set<Sensor> allSensors = getSensors(true, 3);
       when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
       allSensors.forEach(sensor -> securityService.changeSensorActivationStatus(sensor, false));
       verify(securityRepository, times(3)).setAlarmStatus(AlarmStatus.NO_ALARM);
   }

    //Test4
    @Test
    void AlarmActivated_NoChangeInAlarmStatusWhileChangeSensorActivation() {
        // 设置 AlarmStatus 为 ALARM
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);

        sensor.setActive(true);
        securityService.changeSensorActivationStatus(sensor, false);
        verify(securityRepository, times(1)).updateSensor(sensor);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));


        reset(securityRepository);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);

        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, times(1)).updateSensor(sensor);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    //Test5
    @Test
    void ActivatedAndAlreadyActiveAndPending_ChangeToAlarm(){
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor,true);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }
    //Test6
    @Test
    void DeActivatedAndAlreadyInactive_NoChangeToAlarmState(){
        securityService.changeSensorActivationStatus(sensor, false);
        verify(securityRepository, never()).setAlarmStatus(any());
    }


   @Test
    void SensorActivatedAndAlarm_ChangeToPending(){
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
       Set<Sensor> allSensors = getSensors(true, 3);
       allSensors.forEach(sensor -> securityService.changeSensorActivationStatus(sensor));
        verify(securityRepository,times(3)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
   }


    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void setArmingStatusArmed_NoChangeInAlarmStatus(ArmingStatus armingStatus) {
        securityService.setArmingStatus(armingStatus);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }
    @Test
    void PendingAndDeactivateSensor(){
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor);
        verify(securityRepository,times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }
    //Test7
    @Test
    void CatIdentifiedAndArmedHome_ChangeToAlarm(){
        int width = 100;
        int height = 100;
        BufferedImage catImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(eq(catImage), anyFloat())).thenReturn(true);
        securityService.processImage(catImage);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);

    }
    //Test8
    @Test
    void CatUnidentified_ChangeToNoAlarm(){
        int width = 100;
        int height = 100;
        BufferedImage catImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        sensor.setActive(false);
        when(imageService.imageContainsCat(eq(catImage), anyFloat())).thenReturn(false);

        securityService.processImage(catImage);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);

    }

    //Test 9
    @Test
    void setArmingStatusDisarmed_SetAlarmStatusNoAlarm() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }
    //Test10
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void SystemArmed_resetAllSensorToInactive(ArmingStatus armingStatus){

        Set<Sensor> sensors = getSensors(true, 3);
        when(securityRepository.getSensors()).thenReturn(sensors);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.setArmingStatus(armingStatus);
        securityService.getSensors().forEach(sensor -> {
            assertFalse(sensor.getActive());
        });
    }
    //Test 11
    @Test
    void ArmedHomeWhileImageServiceIdentifiesCat_changeStatusToAlarm() {
        int width = 100;
        int height = 100;
        BufferedImage catImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        when(imageService.imageContainsCat(any(),anyFloat())).thenReturn(true);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        securityService.processImage(catImage);
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    void addAndRemoveStatusListener() {
        securityService.addStatusListener(statusListener);
        securityService.removeStatusListener(statusListener);
    }

    @Test
    void addAndRemoveSensor() {
        securityService.addSensor(sensor);
        securityService.removeSensor(sensor);
    }

}



