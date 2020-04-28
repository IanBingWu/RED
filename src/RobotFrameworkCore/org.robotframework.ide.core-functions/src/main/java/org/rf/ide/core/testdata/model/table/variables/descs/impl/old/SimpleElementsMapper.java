/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.testdata.model.table.variables.descs.impl.old;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.rf.ide.core.testdata.model.FilePosition;

class SimpleElementsMapper {

    private final Map<ContainerElementType, IElementMapper> mappers = new HashMap<>();

    SimpleElementsMapper() {
        mappers.put(ContainerElementType.TEXT, new TextDeclarationMapper());
        mappers.put(ContainerElementType.CURLY_BRACKET_CLOSE, new TextDeclarationMapper());
        mappers.put(ContainerElementType.SQUARE_BRACKET_CLOSE, new TextDeclarationMapper());
        mappers.put(ContainerElementType.WHITESPACE, new WhitespaceMapper());
        mappers.put(ContainerElementType.ESCAPE, new EscapeMapper());
        mappers.put(ContainerElementType.VARIABLE_TYPE_ID, new VariableIdentifierMapper());
    }

    interface IElementMapper {

        MappingResult map(final MappingResult currentResult, final IContainerElement containerElement,
                final FilePosition fp);
    }

    IElementMapper getMapperFor(final ContainerElementType type) {
        return mappers.get(type);
    }

    private class TextDeclarationMapper extends AMergeAllowedMapper {

        TextDeclarationMapper() {
            super(Arrays.asList(ContainerElementType.TEXT, ContainerElementType.WHITESPACE));
        }
    }

    private class WhitespaceMapper extends AMergeAllowedMapper {

        WhitespaceMapper() {
            super(Arrays.asList(ContainerElementType.VARIABLE_TYPE_ID, ContainerElementType.ESCAPE,
                    ContainerElementType.TEXT, ContainerElementType.WHITESPACE));
        }
    }

    private abstract class AMergeAllowedMapper implements IElementMapper {

        private final List<ContainerElementType> mergeAllowedTypes;

        AMergeAllowedMapper(final List<ContainerElementType> mergeAllowedTypes) {
            this.mergeAllowedTypes = mergeAllowedTypes;
        }

        @Override
        public MappingResult map(final MappingResult currentResult, final IContainerElement containerElement,
                final FilePosition fp) {
            final MappingResult mr = new MappingResult(fp);
            final List<IElementDeclaration> mappedElements = currentResult.getMappedElements();
            final TextPosition position = ((ContainerElement) containerElement).getPosition();
            final TextDeclaration decText = new TextDeclaration(position, containerElement.getType());

            boolean shouldMapToNew = true;
            if (!mappedElements.isEmpty()) {
                final IElementDeclaration lastMapped = mappedElements.get(mappedElements.size() - 1);
                if (lastMapped instanceof JoinedTextDeclarations) {
                    final JoinedTextDeclarations joined = (JoinedTextDeclarations) lastMapped;
                    if (containsOnly(joined.getElementsDeclarationInside(), mergeAllowedTypes)) {
                        joined.addElementDeclarationInside(decText);
                        shouldMapToNew = false;
                    }
                }
            }

            if (shouldMapToNew) {
                final JoinedTextDeclarations text = new JoinedTextDeclarations();
                text.addElementDeclarationInside(decText);
                mr.addMappedElement(text);
            }

            mr.setLastFilePosition(new FilePosition(fp.getLine(), fp.getColumn() + position.getLength(),
                    fp.getOffset() + position.getLength()));

            return mr;
        }
    }

    private class EscapeMapper implements IElementMapper {

        @Override
        public MappingResult map(final MappingResult currentResult, final IContainerElement containerElement,
                final FilePosition fp) {
            return createNewTextDeclaration(containerElement, fp);
        }

    }

    private class VariableIdentifierMapper implements IElementMapper {

        @Override
        public MappingResult map(final MappingResult currentResult, final IContainerElement containerElement,
                final FilePosition fp) {
            return createNewTextDeclaration(containerElement, fp);
        }
    }

    private MappingResult createNewTextDeclaration(final IContainerElement containerElement, final FilePosition fp) {
        final MappingResult mr = new MappingResult(fp);
        final TextPosition position = ((ContainerElement) containerElement).getPosition();
        final TextDeclaration decText = new TextDeclaration(position, containerElement.getType());
        final JoinedTextDeclarations text = new JoinedTextDeclarations();
        text.addElementDeclarationInside(decText);
        mr.addMappedElement(text);
        mr.setLastFilePosition(new FilePosition(fp.getLine(), fp.getColumn() + position.getLength(),
                fp.getOffset() + position.getLength()));

        return mr;
    }

    private boolean containsOnly(final List<IElementDeclaration> mappedElements,
            final List<ContainerElementType> typesAllowed) {
        boolean result = true;
        for (final IElementDeclaration dec : mappedElements) {
            if (!containsOnly(dec, typesAllowed)) {
                result = false;
                break;
            }
        }
        return result;
    }

    boolean containsOnly(final IElementDeclaration elem, final List<ContainerElementType> typesAllowed) {
        boolean result = true;
        final List<ContainerElementType> types = elem.getTypes();
        for (final ContainerElementType t : types) {
            if (!typesAllowed.contains(t)) {
                result = false;
                break;
            }
        }

        return result;
    }
}
