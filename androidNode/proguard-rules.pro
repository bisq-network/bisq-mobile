# Bisq Node proguard file

## TODO - more node specifics

### protobuf deps
-dontwarn java.lang.MatchException

# Keep gRPC and Netty classes
-keep class io.grpc.** { *; }
-keep class io.netty.** { *; }
-keep class io.grpc.netty.shaded.io.netty.** { *; }

# Keep all the missing classes that are referenced but not directly used
-dontwarn com.aayushatharva.brotli4j.**
-dontwarn com.github.luben.zstd.**
-dontwarn com.google.protobuf.nano.**
-dontwarn com.jcraft.jzlib.**
-dontwarn com.ning.compress.**
-dontwarn com.oracle.svm.core.**
-dontwarn lzma.sdk.**
-dontwarn net.jpountz.lz4.**
-dontwarn net.jpountz.xxhash.**
-dontwarn org.apache.logging.log4j.**
-dontwarn org.bouncycastle.openssl.**
-dontwarn org.bouncycastle.operator.**
-dontwarn org.bouncycastle.pkcs.**
-dontwarn org.conscrypt.**
-dontwarn org.eclipse.jetty.alpn.**
-dontwarn org.eclipse.jetty.npn.**
-dontwarn org.jboss.marshalling.**
-dontwarn reactor.blockhound.**
-dontwarn sun.security.x509.**

# Don't shrink/obfuscate build-time plugins
-dontwarn com.android.build.**
-dontwarn com.google.protobuf.gradle.**
-dontwarn org.codehaus.groovy.**
-dontwarn javax.inject.**
-dontwarn org.gradle.**
-dontwarn org.bouncycastle.**
-dontwarn javassist.**
-dontwarn org.apache.maven.**
-dontwarn kr.motd.maven.**
-dontwarn org.eclipse.**

# Optional: keep core Gradle plugin APIs if somehow referenced
-keep class com.android.** { *; }
-keep class org.gradle.** { *; }

# Keep any native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep all classes that might be used via reflection
-keep class * implements java.io.Serializable { *; }

# Keep all enum values
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep all Bisq2 core classes
-keep class bisq.** { *; }
-keep class network.bisq.** { *; }

# Keep all Protobuf-related classes
-keep class com.google.protobuf.** { *; }

# Keep all Bouncy Castle classes
-keep class org.bouncycastle.** { *; }

# Keep all Tor-related classes
-keep class org.torproject.** { *; }

# Preserve all classes and methods in the kmp-tor package
#-keep class io.matthewnelson.kmp.tor.** { *; }

# Preserve classes with JNI annotations
#-keepclasseswithmembers class * {
#    @android.annotation.Keep *;
#}
#
## Keep kmp-tor resource loading
#-keep class io.matthewnelson.kmp.tor.resource.exec.** { *; }
#-keep class io.matthewnelson.kmp.tor.resource.noexec.** { *; }
#
## Prevent obfuscation of kmp-tor internal classes
#-dontwarn io.matthewnelson.kmp.tor.**

-dontwarn com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector
-dontwarn com.sun.jdi.VirtualMachine
-dontwarn com.sun.jdi.event.Event
-dontwarn com.sun.jdi.event.EventIterator
-dontwarn com.sun.jdi.event.EventQueue
-dontwarn com.sun.jdi.event.EventSet
-dontwarn com.sun.jdi.event.MethodEntryEvent
-dontwarn com.sun.net.httpserver.Headers
-dontwarn com.sun.net.httpserver.HttpExchange
-dontwarn com.sun.net.httpserver.HttpHandler
-dontwarn com.sun.net.httpserver.HttpServer
-dontwarn com.sun.net.httpserver.HttpsConfigurator
-dontwarn com.sun.net.httpserver.HttpsServer
-dontwarn com.sun.xml.fastinfoset.stax.StAXDocumentParser
-dontwarn com.sun.xml.fastinfoset.stax.StAXDocumentSerializer
-dontwarn groovy.lang.Closure
-dontwarn groovy.lang.GroovyObject
-dontwarn groovy.lang.MetaClass
-dontwarn groovy.transform.Generated
-dontwarn jakarta.servlet.http.HttpServlet
-dontwarn java.awt.Button
-dontwarn java.awt.Checkbox
-dontwarn java.awt.CheckboxGroup
-dontwarn java.awt.Component
-dontwarn java.awt.Dialog
-dontwarn java.awt.Frame
-dontwarn java.awt.Graphics2D
-dontwarn java.awt.Graphics
-dontwarn java.awt.Image
-dontwarn java.awt.Label
-dontwarn java.awt.MediaTracker
-dontwarn java.awt.TextArea
-dontwarn java.awt.event.ActionListener
-dontwarn java.awt.event.ItemListener
-dontwarn java.awt.event.WindowListener
-dontwarn java.awt.image.BufferedImage
-dontwarn java.awt.image.ImageObserver
-dontwarn java.awt.image.RenderedImage
-dontwarn java.beans.BeanInfo
-dontwarn java.beans.ConstructorProperties
-dontwarn java.beans.FeatureDescriptor
-dontwarn java.beans.IntrospectionException
-dontwarn java.beans.Introspector
-dontwarn java.beans.PropertyDescriptor
-dontwarn java.beans.Transient
-dontwarn java.lang.Module
-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean
-dontwarn java.lang.reflect.AnnotatedType
-dontwarn javax.imageio.ImageIO
-dontwarn javax.imageio.ImageReadParam
-dontwarn javax.imageio.ImageReader
-dontwarn javax.imageio.ImageWriter
-dontwarn javax.imageio.spi.ImageWriterSpi
-dontwarn javax.imageio.stream.ImageInputStream
-dontwarn javax.imageio.stream.ImageOutputStream
-dontwarn javax.mail.Address
-dontwarn javax.mail.Authenticator
-dontwarn javax.mail.BodyPart
-dontwarn javax.mail.Message$RecipientType
-dontwarn javax.mail.Message
-dontwarn javax.mail.Multipart
-dontwarn javax.mail.Session
-dontwarn javax.mail.Transport
-dontwarn javax.mail.internet.AddressException
-dontwarn javax.mail.internet.InternetAddress
-dontwarn javax.mail.internet.MimeBodyPart
-dontwarn javax.mail.internet.MimeMessage
-dontwarn javax.mail.internet.MimeMultipart
-dontwarn javax.management.DynamicMBean
-dontwarn javax.management.InstanceNotFoundException
-dontwarn javax.management.JMException
-dontwarn javax.management.MBeanAttributeInfo
-dontwarn javax.management.MBeanConstructorInfo
-dontwarn javax.management.MBeanInfo
-dontwarn javax.management.MBeanNotificationInfo
-dontwarn javax.management.MBeanOperationInfo
-dontwarn javax.management.MBeanRegistrationException
-dontwarn javax.management.MBeanServer
-dontwarn javax.management.MalformedObjectNameException
-dontwarn javax.management.ObjectInstance
-dontwarn javax.management.ObjectName
-dontwarn javax.management.QueryExp
-dontwarn javax.naming.Context
-dontwarn javax.naming.InitialContext
-dontwarn javax.naming.InvalidNameException
-dontwarn javax.naming.NamingEnumeration
-dontwarn javax.naming.NamingException
-dontwarn javax.naming.directory.Attribute
-dontwarn javax.naming.directory.Attributes
-dontwarn javax.naming.directory.DirContext
-dontwarn javax.naming.directory.InitialDirContext
-dontwarn javax.naming.directory.SearchControls
-dontwarn javax.naming.directory.SearchResult
-dontwarn javax.naming.ldap.LdapName
-dontwarn javax.naming.ldap.Rdn
-dontwarn javax.servlet.Filter
-dontwarn javax.servlet.ServletContainerInitializer
-dontwarn javax.servlet.ServletContextListener
-dontwarn javax.servlet.http.HttpServlet
-dontwarn javax.xml.stream.Location
-dontwarn javax.xml.stream.XMLEventFactory
-dontwarn javax.xml.stream.XMLEventWriter
-dontwarn javax.xml.stream.XMLStreamException
-dontwarn javax.xml.stream.XMLStreamReader
-dontwarn javax.xml.stream.XMLStreamWriter
-dontwarn javax.xml.stream.events.Attribute
-dontwarn javax.xml.stream.events.Characters
-dontwarn javax.xml.stream.events.EndDocument
-dontwarn javax.xml.stream.events.EndElement
-dontwarn javax.xml.stream.events.Namespace
-dontwarn javax.xml.stream.events.StartDocument
-dontwarn javax.xml.stream.events.StartElement
-dontwarn javax.xml.stream.events.XMLEvent
-dontwarn org.apache.avalon.framework.logger.Logger
-dontwarn org.apache.log.Hierarchy
-dontwarn org.apache.log.Logger
-dontwarn org.apache.log4j.Level
-dontwarn org.apache.log4j.Logger
-dontwarn org.apache.log4j.Priority
-dontwarn org.apache.commons.logging.impl.Log4JLogger
-keep class org.apache.commons.logging.impl.Log4JLogger { *; }
-dontwarn org.apache.maven.AbstractMavenLifecycleParticipant
-dontwarn org.apache.maven.plugin.AbstractMojo
-dontwarn org.apache.maven.plugins.annotations.LifecyclePhase
-dontwarn org.apache.maven.plugins.annotations.Mojo
-dontwarn org.brotli.dec.BrotliInputStream
-dontwarn org.codehaus.groovy.reflection.ClassInfo
-dontwarn org.codehaus.groovy.runtime.GeneratedClosure
-dontwarn org.codehaus.groovy.runtime.ScriptBytecodeAdapter
-dontwarn org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation
-dontwarn org.codehaus.groovy.runtime.typehandling.ShortTypeHandling
-dontwarn org.codehaus.groovy.transform.ImmutableASTTransformation
-dontwarn org.codehaus.janino.ClassBodyEvaluator
-dontwarn org.codehaus.janino.ScriptEvaluator
-dontwarn org.codehaus.plexus.component.annotations.Component
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.eclipse.ui.IStartup
-dontwarn org.graalvm.nativeimage.hosted.Feature
-dontwarn org.gradle.api.Action
-dontwarn org.gradle.api.DefaultTask
-dontwarn org.gradle.api.Named
-dontwarn org.gradle.api.Plugin
-dontwarn org.gradle.api.Task
-dontwarn org.gradle.api.artifacts.Dependency
-dontwarn org.gradle.api.artifacts.ExternalModuleDependency
-dontwarn org.gradle.api.artifacts.dsl.DependencyHandler
-dontwarn org.gradle.api.attributes.Attribute
-dontwarn org.gradle.api.attributes.AttributeCompatibilityRule
-dontwarn org.gradle.api.attributes.AttributeContainer
-dontwarn org.gradle.api.attributes.AttributeDisambiguationRule
-dontwarn org.gradle.api.attributes.HasAttributes
-dontwarn org.gradle.api.component.SoftwareComponent
-dontwarn org.gradle.api.plugins.ExtensionAware
-dontwarn org.gradle.api.tasks.CacheableTask
-dontwarn org.gradle.api.tasks.util.PatternFilterable
-dontwarn org.gradle.util.GradleVersion
-dontwarn org.hibernate.validator.HibernateValidator
-dontwarn org.ietf.jgss.GSSContext
-dontwarn org.ietf.jgss.GSSCredential
-dontwarn org.ietf.jgss.GSSException
-dontwarn org.ietf.jgss.GSSManager
-dontwarn org.ietf.jgss.GSSName
-dontwarn org.ietf.jgss.Oid
-dontwarn org.jvnet.fastinfoset.VocabularyApplicationData
-dontwarn org.jvnet.staxex.XMLStreamWriterEx
-dontwarn org.osgi.framework.Bundle
-dontwarn org.osgi.framework.BundleActivator
-dontwarn org.osgi.framework.BundleContext
-dontwarn org.osgi.framework.BundleListener
-dontwarn org.osgi.framework.BundleReference
-dontwarn org.osgi.framework.FrameworkUtil
-dontwarn org.osgi.framework.SynchronousBundleListener
-dontwarn sun.reflect.Reflection

## General

# Keep Kotlin Metadata
-keepattributes KotlinMetadata

# Keep KMP Framework Class Names
-keep class kotlinx.** { *; }

# Avoid stripping enums used by KMP
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Compose Compiler Intrinsics
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.foundation.** { *; }
-keep class androidx.compose.material.** { *; }

# Keep Compose Preview Annotations (if using Android Studio Preview)
-keep @androidx.compose.ui.tooling.preview.Preview class * { *; }

# Keep Composer Intrinsics
-keep class androidx.compose.runtime.internal.ComposableLambdaImpl { *; }

# Keep all classes annotated with @Composable
-keep class * {
    @androidx.compose.runtime.Composable *;
}

# Preserve classes used in reflection (Compose & tor)
-keepattributes *Annotation*, InnerClasses, Signature

# Keep Jetpack Compose runtime classes
-keep class androidx.compose.** { *; }

# Keep Kotlin metadata
-keep class kotlin.Metadata { *; }
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Keep Koin classes and avoid stripping DI components
-keep class org.koin.** { *; }
-keepclassmembers class * {
    @org.koin.core.annotation.* <fields>;
    @org.koin.core.annotation.* <methods>;
}