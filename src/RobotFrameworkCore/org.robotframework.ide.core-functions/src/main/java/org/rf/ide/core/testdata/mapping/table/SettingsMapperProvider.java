/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.testdata.mapping.table;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.rf.ide.core.environment.RobotVersion;
import org.rf.ide.core.testdata.mapping.HashCommentMapper;
import org.rf.ide.core.testdata.mapping.setting.DefaultTagsMapper;
import org.rf.ide.core.testdata.mapping.setting.DefaultTagsTagNameMapper;
import org.rf.ide.core.testdata.mapping.setting.ForceTagsMapper;
import org.rf.ide.core.testdata.mapping.setting.ForceTagsTagNameMapper;
import org.rf.ide.core.testdata.mapping.setting.MetadataKeyMapper;
import org.rf.ide.core.testdata.mapping.setting.MetadataMapper;
import org.rf.ide.core.testdata.mapping.setting.MetadataValueMapper;
import org.rf.ide.core.testdata.mapping.setting.SettingDocumentationMapper;
import org.rf.ide.core.testdata.mapping.setting.SettingDocumentationTextMapper;
import org.rf.ide.core.testdata.mapping.setting.imports.LibraryArgumentsMapper;
import org.rf.ide.core.testdata.mapping.setting.imports.LibraryDeclarationMapper;
import org.rf.ide.core.testdata.mapping.setting.imports.LibraryNameOrPathMapper;
import org.rf.ide.core.testdata.mapping.setting.imports.ResourceDeclarationMapper;
import org.rf.ide.core.testdata.mapping.setting.imports.ResourceImportPathMapper;
import org.rf.ide.core.testdata.mapping.setting.imports.ResourceTrashDataMapper;
import org.rf.ide.core.testdata.mapping.setting.imports.VariablesArgumentsMapper;
import org.rf.ide.core.testdata.mapping.setting.imports.VariablesDeclarationMapper;
import org.rf.ide.core.testdata.mapping.setting.imports.VariablesImportPathMapper;
import org.rf.ide.core.testdata.mapping.setting.suite.SuiteSetupKeywordArgumentMapper;
import org.rf.ide.core.testdata.mapping.setting.suite.SuiteSetupKeywordMapper;
import org.rf.ide.core.testdata.mapping.setting.suite.SuiteSetupMapper;
import org.rf.ide.core.testdata.mapping.setting.suite.SuiteTeardownKeywordArgumentMapper;
import org.rf.ide.core.testdata.mapping.setting.suite.SuiteTeardownKeywordMapper;
import org.rf.ide.core.testdata.mapping.setting.suite.SuiteTeardownMapper;
import org.rf.ide.core.testdata.mapping.setting.task.TaskSetupKeywordArgumentMapper;
import org.rf.ide.core.testdata.mapping.setting.task.TaskSetupKeywordMapper;
import org.rf.ide.core.testdata.mapping.setting.task.TaskSetupMapper;
import org.rf.ide.core.testdata.mapping.setting.task.TaskTeardownKeywordArgumentMapper;
import org.rf.ide.core.testdata.mapping.setting.task.TaskTeardownKeywordMapper;
import org.rf.ide.core.testdata.mapping.setting.task.TaskTeardownMapper;
import org.rf.ide.core.testdata.mapping.setting.task.TaskTemplateKeywordMapper;
import org.rf.ide.core.testdata.mapping.setting.task.TaskTemplateMapper;
import org.rf.ide.core.testdata.mapping.setting.task.TaskTemplateTrashDataMapper;
import org.rf.ide.core.testdata.mapping.setting.task.TaskTimeoutMapper;
import org.rf.ide.core.testdata.mapping.setting.task.TaskTimeoutMessageMapper;
import org.rf.ide.core.testdata.mapping.setting.task.TaskTimeoutValueMapper;
import org.rf.ide.core.testdata.mapping.setting.test.TestSetupKeywordArgumentMapper;
import org.rf.ide.core.testdata.mapping.setting.test.TestSetupKeywordMapper;
import org.rf.ide.core.testdata.mapping.setting.test.TestSetupMapper;
import org.rf.ide.core.testdata.mapping.setting.test.TestTeardownKeywordArgumentMapper;
import org.rf.ide.core.testdata.mapping.setting.test.TestTeardownKeywordMapper;
import org.rf.ide.core.testdata.mapping.setting.test.TestTeardownMapper;
import org.rf.ide.core.testdata.mapping.setting.test.TestTemplateKeywordMapper;
import org.rf.ide.core.testdata.mapping.setting.test.TestTemplateMapper;
import org.rf.ide.core.testdata.mapping.setting.test.TestTemplateTrashDataMapper;
import org.rf.ide.core.testdata.mapping.setting.test.TestTimeoutMapper;
import org.rf.ide.core.testdata.mapping.setting.test.TestTimeoutMessageMapper;
import org.rf.ide.core.testdata.mapping.setting.test.TestTimeoutValueMapper;


public class SettingsMapperProvider {

    private static final List<IParsingMapper> MAPPERS = Arrays.asList(
            new GarbageBeforeFirstTableMapper(),

            new TableHeaderColumnMapper(),

            new HashCommentMapper(),

            new LibraryDeclarationMapper(), new LibraryNameOrPathMapper(), new LibraryArgumentsMapper(),

            new VariablesDeclarationMapper(), new VariablesImportPathMapper(), new VariablesArgumentsMapper(),

            new ResourceDeclarationMapper(), new ResourceImportPathMapper(), new ResourceTrashDataMapper(),

            new SettingDocumentationMapper(), new SettingDocumentationTextMapper(),

            new MetadataMapper(), new MetadataKeyMapper(), new MetadataValueMapper(),

            new SuiteSetupMapper(), new SuiteSetupKeywordMapper(), new SuiteSetupKeywordArgumentMapper(),

            new SuiteTeardownMapper(), new SuiteTeardownKeywordMapper(), new SuiteTeardownKeywordArgumentMapper(),

            new ForceTagsMapper(), new ForceTagsTagNameMapper(),

            new DefaultTagsMapper(), new DefaultTagsTagNameMapper(),

            new TestSetupMapper(), new TestSetupKeywordMapper(), new TestSetupKeywordArgumentMapper(),

            new TestTeardownMapper(), new TestTeardownKeywordMapper(), new TestTeardownKeywordArgumentMapper(),

            new TestTemplateMapper(), new TestTemplateKeywordMapper(), new TestTemplateTrashDataMapper(),

            new TestTimeoutMapper(), new TestTimeoutValueMapper(), new TestTimeoutMessageMapper(),

            new TaskSetupMapper(), new TaskSetupKeywordMapper(), new TaskSetupKeywordArgumentMapper(),

            new TaskTeardownMapper(), new TaskTeardownKeywordMapper(), new TaskTeardownKeywordArgumentMapper(),

            new TaskTemplateMapper(), new TaskTemplateKeywordMapper(), new TaskTemplateTrashDataMapper(),

            new TaskTimeoutMapper(), new TaskTimeoutValueMapper(), new TaskTimeoutMessageMapper());


    public List<IParsingMapper> getMappers(final RobotVersion robotVersion) {
        final List<IParsingMapper> mappers = new ArrayList<>();
        for (final IParsingMapper mapper : MAPPERS) {
            if (mapper.isApplicableFor(robotVersion)) {
                mappers.add(mapper);
            }
        }
        return mappers;
    }
}
