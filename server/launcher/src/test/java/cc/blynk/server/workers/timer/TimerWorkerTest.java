package cc.blynk.server.workers.timer;

import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.dao.UserDao;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.Profile;
import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.widgets.controls.Timer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.Mockito.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/6/2015.
 */
@RunWith(MockitoJUnitRunner.class)
public class TimerWorkerTest {

    @Mock
    private UserDao userDao;

    @Mock
    private SessionDao sessionDao;

    @Spy
    @InjectMocks
    private TimerWorker timerWorker;

    @Mock
    private User user;

    @Mock
    private Profile profile;

    private ConcurrentHashMap<String, User> users = new ConcurrentHashMap<>();

    private ConcurrentHashMap<User, Session> userSession = new ConcurrentHashMap<>();

    private List<Timer> timers = new ArrayList<>();
    private Timer w = new Timer();

    @Before
    public void init() {
        timers.add(w);
    }

    @Test
    public void testTimer() {
        //wait for start of a second
        long startDelay = 1001 - (System.currentTimeMillis() % 1000);
        try {
            Thread.sleep(startDelay);
        } catch (InterruptedException e) {
        }

        LocalDateTime localDateTime = LocalDateTime.now(ZoneId.of("UTC"));
        long curTime = localDateTime.getSecond() + localDateTime.getMinute() * 60 + localDateTime.getHour() * 3600;
        w.startTime = curTime;

        int userCount = 1000;
        for (int i = 0; i < userCount; i++) {
            users.put(String.valueOf(i), user);
        }

        when(userDao.getUsers()).thenReturn(users);
        when(sessionDao.getUserSession()).thenReturn(userSession);
        user.profile = profile;
        profile.dashBoards = new DashBoard[] {};
        when(profile.getActiveTimerWidgets()).thenReturn(timers);

        timerWorker.run();

        verify(timerWorker, times(1000)).timerTick(eq(curTime), eq(curTime));
    }

}
