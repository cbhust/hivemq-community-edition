/*
 * Copyright 2019 dc-square GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hivemq.extensions.services.auth;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.auth.Authenticator;
import com.hivemq.extension.sdk.api.auth.SimpleAuthenticator;
import com.hivemq.extension.sdk.api.auth.parameter.AuthenticatorProviderInput;
import com.hivemq.extension.sdk.api.auth.parameter.SimpleAuthInput;
import com.hivemq.extension.sdk.api.auth.parameter.SimpleAuthOutput;
import com.hivemq.extension.sdk.api.services.auth.provider.AuthenticatorProvider;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.classloader.IsolatedPluginClassloader;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import util.IsolatedPluginClassLoaderUtil;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

/**
 * @author Georg Held
 */
@SuppressWarnings("NullabilityAnnotations")
public class SecurityRegistryImplTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private HiveMQExtensions hiveMQExtensions;

    @Mock
    private HiveMQExtension hiveMQExtension;

    private Authenticator authenticator1;
    private Authenticator authenticator2;
    private AuthenticatorProvider provider1;
    private AuthenticatorProvider provider2;
    private Authenticators authenticators;
    private Authorizers authorizers;
    private SecurityRegistryImpl securityRegistry;

    @Before
    public void setUp() throws Exception {

        MockitoAnnotations.initMocks(this);

        authenticators = new AuthenticatorsImpl(hiveMQExtensions);
        authorizers = new AuthorizersImpl(hiveMQExtensions);
        securityRegistry = new SecurityRegistryImpl(authenticators, authorizers, hiveMQExtensions);

        when(hiveMQExtensions.getExtensionForClassloader(any(IsolatedPluginClassloader.class)))
                .thenReturn(hiveMQExtension);
        when(hiveMQExtensions.getExtension(anyString())).thenReturn(hiveMQExtension);

        when(hiveMQExtension.getId()).thenReturn("extension1");
        when(hiveMQExtension.getPriority()).thenReturn(1);

        final IsolatedPluginClassloader classloader = IsolatedPluginClassLoaderUtil.buildClassLoader(temporaryFolder,
                new Class[]{TestProvider1.class, TestProvider2.class, TestSimpleAuthenticator.class});

        provider1 = IsolatedPluginClassLoaderUtil.instanceFromClassloader(classloader, TestProvider1.class);
        provider2 = IsolatedPluginClassLoaderUtil.instanceFromClassloader(classloader, TestProvider2.class);
        authenticator1 = provider1.getAuthenticator(null);
        authenticator2 = provider2.getAuthenticator(null);
    }

    @Test
    public void test_set_authenticator_provider() {

        securityRegistry.setAuthenticatorProvider(provider1);

        final Map<String, WrappedAuthenticatorProvider> registeredAuthenticators = authenticators.getAuthenticatorProviderMap();

        assertEquals(1, registeredAuthenticators.size());
        assertSame(authenticator1, registeredAuthenticators.values().iterator().next().getAuthenticator(null));
    }

    @Test(timeout = 5000)
    public void test_set_second_authenticator_provider_from_same_classloader() {

        securityRegistry.setAuthenticatorProvider(provider1);
        Map<String, WrappedAuthenticatorProvider> registeredAuthenticators = authenticators.getAuthenticatorProviderMap();
        assertEquals(1, registeredAuthenticators.size());
        assertSame(authenticator1, registeredAuthenticators.values().iterator().next().getAuthenticator(null));

        //replace authenticator
        securityRegistry.setAuthenticatorProvider(provider2);
        registeredAuthenticators = authenticators.getAuthenticatorProviderMap();
        assertEquals(1, registeredAuthenticators.size());
        assertSame(authenticator2, registeredAuthenticators.values().iterator().next().getAuthenticator(null));

    }

    @Test(timeout = 5000, expected = NullPointerException.class)
    public void test_set_null_authenticator_provider() {
        securityRegistry.setAuthenticatorProvider(null);
    }

    public static class TestProvider1 implements AuthenticatorProvider {

        private final Authenticator authenticator;

        public TestProvider1() {
            authenticator = new TestSimpleAuthenticator();
        }

        @Override
        public @Nullable Authenticator getAuthenticator(
                @NotNull final AuthenticatorProviderInput authenticatorProviderInput) {
            return authenticator;
        }
    }

    public static class TestProvider2 implements AuthenticatorProvider {

        private final Authenticator authenticator;

        public TestProvider2() {
            authenticator = new TestSimpleAuthenticator();
        }

        @Override
        public @Nullable Authenticator getAuthenticator(
                @NotNull final AuthenticatorProviderInput authenticatorProviderInput) {
            return authenticator;
        }
    }

    public static class TestSimpleAuthenticator implements SimpleAuthenticator {

        @Override
        public void onConnect(
                @NotNull final SimpleAuthInput input, @NotNull final SimpleAuthOutput output) {
            //noop
        }
    }
}