/*
 * Copyright 2016 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.testdata.model.table.exec.descs;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.rf.ide.core.environment.RobotVersion;
import org.rf.ide.core.testdata.model.RobotFile;
import org.rf.ide.core.testdata.model.RobotFileOutput;
import org.rf.ide.core.testdata.model.table.KeywordTable;
import org.rf.ide.core.testdata.model.table.RobotExecutableRow;
import org.rf.ide.core.testdata.model.table.exec.descs.impl.ForLoopContinueRowDescriptor;
import org.rf.ide.core.testdata.model.table.exec.descs.impl.ForLoopDeclarationRowDescriptor;
import org.rf.ide.core.testdata.model.table.exec.descs.impl.SimpleRowDescriptor;
import org.rf.ide.core.testdata.model.table.keywords.UserKeyword;
import org.rf.ide.core.testdata.model.table.variables.descs.VariableUse;
import org.rf.ide.core.testdata.text.read.recognizer.RobotToken;
import org.rf.ide.core.testdata.text.read.recognizer.RobotTokenType;

/**
 * @author wypych
 */
public class ForLoopDeclarationRowDescriptorBuilderTest {

    @Test
    public void test_ifBuildExecutableWillUseCopyObjects_containsOneForAndOneForContinueRows_keywordTable() {
        // given
        final RobotFileOutput out = new RobotFileOutput(RobotVersion.from("3.0"));
        out.setProcessedFile(new File("robot.robot"));
        final RobotFile model = new RobotFile(out);
        model.includeKeywordTableSection();
        final KeywordTable keywordTable = model.getKeywordTable();
        final UserKeyword userKeyword = keywordTable.createUserKeyword("dbfoo");
        final RobotExecutableRow<UserKeyword> forExec = new RobotExecutableRow<>();
        final RobotExecutableRow<UserKeyword> forContinueOneExec = new RobotExecutableRow<>();
        userKeyword.addElement(forExec);
        userKeyword.addElement(forContinueOneExec);

        forExec.setAction(RobotToken.create(":FOR", 1, 2, RobotTokenType.FOR_TOKEN));
        forExec.addArgument(RobotToken.create("${i}"));
        forExec.addArgument(RobotToken.create("in range"));
        forExec.addArgument(RobotToken.create("${c}"));

        final RobotToken forContinueAction = RobotToken.create("\\");
        forContinueAction.setLineNumber(2);
        forContinueOneExec.setAction(forContinueAction);
        forContinueOneExec.addArgument(RobotToken.create("Log"));
        forContinueOneExec.addArgument(RobotToken.create("${i}"));

        // when
        final IExecutableRowDescriptor<UserKeyword> forLine = userKeyword.getExecutionContext()
                .get(0)
                .buildLineDescription();
        final IExecutableRowDescriptor<UserKeyword> forContinueOneLine = userKeyword.getExecutionContext()
                .get(1)
                .buildLineDescription();

        // then
        assertThatIsJustCopyNotInstances(forLine, forExec);
        assertThatIsJustCopyNotInstances(forContinueOneLine, forContinueOneExec);
    }

    private void assertThatIsJustCopyNotInstances(final IExecutableRowDescriptor<?> desc,
            final RobotExecutableRow<?> line) {
        final List<RobotToken> elementTokensDesc = line(desc);
        final List<RobotToken> elementTokensLine = line.getElementTokens();

        assertThat(elementTokensDesc.size()).isEqualTo(elementTokensLine.size());
        final int size = elementTokensDesc.size();
        for (int i = 0; i < size; i++) {
            assertThat(elementTokensDesc.get(i) != elementTokensLine.get(i)).isTrue();
        }
    }

    private List<RobotToken> line(final IExecutableRowDescriptor<?> desc) {
        if (desc instanceof SimpleRowDescriptor) {
            throw new UnsupportedOperationException("Not implemented for current test.");
        } else if (desc instanceof ForLoopDeclarationRowDescriptor) {
            return line((ForLoopDeclarationRowDescriptor<?>) desc);
        } else if (desc instanceof ForLoopContinueRowDescriptor) {
            return line((ForLoopContinueRowDescriptor<?>) desc);
        }

        return null;
    }

    private List<RobotToken> line(final ForLoopDeclarationRowDescriptor<?> descriptor) {
        final List<RobotToken> done = new ArrayList<>();
        done.add(descriptor.getAction());
        descriptor.getCreatingVariables().forEach(done::add);
        done.add(descriptor.getInAction());
        for (final VariableUse varDecUsed : descriptor.getUsedVariables()) {
            done.add(varDecUsed.asToken());
        }

        return done;
    }

    private List<RobotToken> line(final ForLoopContinueRowDescriptor<?> descriptor) {
        final List<RobotToken> done = new ArrayList<>();
        done.add(descriptor.getAction());
        descriptor.getCreatingVariables().forEach(done::add);
        done.add(descriptor.getKeywordAction());
        for (final VariableUse varDecUsed : descriptor.getUsedVariables()) {
            done.add(varDecUsed.asToken());
        }
        return done;
    }

}
