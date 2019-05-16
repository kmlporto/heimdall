package br.com.conductor.heimdall.core.service;

/*-
 * =========================LICENSE_START==================================
 * heimdall-core
 * ========================================================================
 * Copyright (C) 2018 Conductor Tecnologia SA
 * ========================================================================
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ==========================LICENSE_END===================================
 */

import br.com.conductor.heimdall.core.dto.AppDTO;
import br.com.conductor.heimdall.core.dto.ReferenceIdDTO;
import br.com.conductor.heimdall.core.dto.persist.AppPersist;
import br.com.conductor.heimdall.core.entity.AccessToken;
import br.com.conductor.heimdall.core.entity.App;
import br.com.conductor.heimdall.core.entity.Developer;
import br.com.conductor.heimdall.core.entity.Plan;
import br.com.conductor.heimdall.core.exception.BadRequestException;
import br.com.conductor.heimdall.core.repository.AccessTokenRepository;
import br.com.conductor.heimdall.core.repository.AppRepository;
import br.com.conductor.heimdall.core.repository.DeveloperRepository;
import br.com.conductor.heimdall.core.service.amqp.AMQPCacheService;
import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="https://dijalmasilva.github.io" target="_blank">Dijalma Silva</a>
 */
@RunWith(MockitoJUnitRunner.class)
public class AppServiceTest {

    @InjectMocks
    private AppService appService;

    @Mock
    private AppRepository appRepository;

    @Mock
    private DeveloperRepository devRepository;

    @Mock
    private AMQPCacheService amqpCacheService;
    
    @Mock
    private AccessTokenRepository accessTokenRepository;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private App app;
    private App app1;
    private Developer developer;
    private AppPersist appPersist;
    private AppDTO appDTO;
    private List<ReferenceIdDTO> referenceIdDTOList = Lists.newArrayList(new ReferenceIdDTO(0L));

    @Before
    public void initAttributes() {
        app = new App();
        app.setId(1L);
        app.setClientId("0a9s8df76g");

        app1 = new App();
        app1.setId(2L);
        app1.setClientId("0q9w8e7r6t");

        developer = new Developer();
        developer.setId(1L);
        developer.setName("dev");
        developer.setEmail("dev@email.com");

        appPersist = new AppPersist();
        appPersist.setName("App test");
        appPersist.setDescription("App test description");
        appPersist.setDeveloper(new ReferenceIdDTO(1L));
    
        appDTO = new AppDTO();
        appDTO.setName("App name");
        appDTO.setDeveloper(new ReferenceIdDTO(1l));
    }

    @Test
    public void testSaveWithCodeAutoGenerate() {

        Mockito.when(appRepository.findByClientId(Mockito.anyString())).thenReturn(null);
        Mockito.when(appRepository.findByClientId("0q9w8e7r6t")).thenReturn(app1);
        Mockito.when(appRepository.save(Mockito.any(App.class))).thenReturn(app);
        Mockito.when(devRepository.findOne(Mockito.anyLong())).thenReturn(developer);

        App saved = this.appService.save(appPersist);

        Assert.assertEquals(app.getId(), saved.getId());
    }

    @Test
    public void testSaveWithCodeInformed() {

        appPersist.setClientId("8d19n91d8");

        Mockito.when(appRepository.findByClientId(Mockito.anyString())).thenReturn(null);
        Mockito.when(appRepository.findByClientId("0q9w8e7r6t")).thenReturn(app1);
        Mockito.when(appRepository.save(Mockito.any(App.class))).thenReturn(app);
        Mockito.when(devRepository.findOne(Mockito.anyLong())).thenReturn(developer);

        App saved = this.appService.save(appPersist);

        Assert.assertEquals(app.getId(), saved.getId());
    }

    @Test
    public void testSaveWithCodeIsAlready() {

        thrown.expect(BadRequestException.class);
        thrown.expectMessage("clientId already used");

        appPersist.setClientId("0q9w8e7r6t");

        Mockito.when(appRepository.findByClientId(Mockito.anyString())).thenReturn(null);
        Mockito.when(appRepository.findByClientId("0q9w8e7r6t")).thenReturn(app1);
        Mockito.when(appRepository.save(Mockito.any(App.class))).thenReturn(app);
        Mockito.when(devRepository.findOne(Mockito.anyLong())).thenReturn(developer);

        this.appService.save(appPersist);
    }
    
    @Test
    public void testUpdateHavingPlans() {
        Mockito.when(appRepository.findOne(Mockito.anyLong())).thenReturn(app);
        Mockito.when(appRepository.save(Mockito.any(App.class))).thenReturn(app);
        Mockito.when(accessTokenRepository.findByAppId(Mockito.anyLong())).thenReturn(Lists.newArrayList(getAccessToken(Lists.newArrayList(getPlan()))));
        
        appDTO.setPlans(referenceIdDTOList);
        
        this.appService.update(0L, appDTO);
        Mockito.verify(accessTokenRepository, Mockito.times(2)).findByAppId(Mockito.anyLong());
        Mockito.verify(accessTokenRepository, Mockito.times(1)).save(Mockito.any(AccessToken.class));
    }
    
    
    @Test
    public void testUpdateHavingNoAccessToken() {
        Mockito.when(appRepository.findOne(Mockito.anyLong())).thenReturn(app);
        Mockito.when(appRepository.save(Mockito.any(App.class))).thenReturn(app);
        Mockito.when(accessTokenRepository.findByAppId(Mockito.anyLong())).thenReturn(null);
        
        appDTO.setPlans(referenceIdDTOList);
        
        this.appService.update(0L, appDTO);
        Mockito.verify(accessTokenRepository, Mockito.times(0)).save(Mockito.any(AccessToken.class));
    }
    
    @Test
    public void testUpdateHavingEmptyAccessToken() {
        Mockito.when(appRepository.findOne(Mockito.anyLong())).thenReturn(app);
        Mockito.when(appRepository.save(Mockito.any(App.class))).thenReturn(app);
        Mockito.when(accessTokenRepository.findByAppId(Mockito.anyLong())).thenReturn(new ArrayList<>());
        
        appDTO.setPlans(referenceIdDTOList);
        
        this.appService.update(0L, appDTO);
        Mockito.verify(accessTokenRepository, Mockito.times(0)).save(Mockito.any(AccessToken.class));
    }
    
    
    private Plan getPlan() {
        
        Plan plan = new Plan();
        plan.setId(1l);
        return plan;
    }
    
    private AccessToken getAccessToken(List<Plan> plans) {
        
        AccessToken accessToken = new AccessToken();
        accessToken.setPlans(plans);
        accessToken.setId(0L);
        return accessToken;
    }
}
