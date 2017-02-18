/*
 * Copyright (c) 2017 by Gerrit Grunwald
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.hansolo.tilesfx.skins;

import eu.hansolo.tilesfx.Tile;
import eu.hansolo.tilesfx.events.ChartDataEventListener;
import eu.hansolo.tilesfx.events.UpdateEvent;
import eu.hansolo.tilesfx.fonts.Fonts;
import eu.hansolo.tilesfx.tools.RadialChartData;
import eu.hansolo.tilesfx.tools.Helper;
import javafx.collections.ListChangeListener;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;


/**
 * Created by hansolo on 17.02.17.
 */
public class RadialChartTileSkin extends TileSkin {
    private Text                                titleText;
    private Text                                text;
    private Canvas                              canvas;
    private GraphicsContext                     ctx;
    private ListChangeListener<RadialChartData> chartDataListener;
    private ChartDataEventListener              chartEventListener;


    // ******************** Constructors **************************************
    public RadialChartTileSkin(final Tile TILE) {
        super(TILE);
    }


    // ******************** Initialization ************************************
    @Override protected void initGraphics() {
        super.initGraphics();

        chartEventListener = e -> drawChart();
        tile.getRadialChartData().forEach(chartData -> chartData.addChartDataEventListener(chartEventListener));

        chartDataListener  = c -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    c.getAddedSubList().forEach(addedItem -> addedItem.addChartDataEventListener(chartEventListener));
                } else if (c.wasRemoved()) {
                    c.getRemoved().forEach(removedItem -> removedItem.removeChartDataEventListener(chartEventListener));
                }
            }
            drawChart();
        };

        titleText = new Text();
        titleText.setFill(tile.getTitleColor());
        Helper.enableNode(titleText, !tile.getTitle().isEmpty());

        text = new Text(tile.getText());
        text.setFill(tile.getTextColor());
        Helper.enableNode(text, tile.isTextVisible());

        canvas = new Canvas(size * 0.9, tile.isTextVisible() ? size * 0.72 : size * 0.795);
        ctx    = canvas.getGraphicsContext2D();

        getPane().getChildren().addAll(titleText, canvas, text);
    }

    @Override protected void registerListeners() {
        super.registerListeners();
        tile.getRadialChartData().addListener(chartDataListener);
    }


    // ******************** Methods *******************************************
    @Override protected void handleEvents(final String EVENT_TYPE) {
        super.handleEvents(EVENT_TYPE);

        if ("VISIBILITY".equals(EVENT_TYPE)) {
            Helper.enableNode(titleText, !tile.getTitle().isEmpty());
            Helper.enableNode(text, tile.isTextVisible());
            canvas.setWidth(tile.isTextVisible() ? size * 0.68 : size * 0.795);
            canvas.setHeight(tile.isTextVisible() ? size * 0.68 : size * 0.795);
        }
    };

    @Override public void dispose() {
        tile.getRadialChartData().removeListener(chartDataListener);
        tile.getRadialChartData().forEach(chartData -> chartData.removeChartDataEventListener(chartEventListener));
        super.dispose();
    }

    private void drawChart() {
        double                canvasSize     = canvas.getWidth();
        double                radius         = canvasSize * 0.5;
        double                innerSpacer    = radius * 0.18;
        double                barWidth       = (radius - innerSpacer) / tile.getRadialChartData().size();
        //List<RadialChartData> sortedDataList = tile.getRadialChartData().stream().sorted(Comparator.comparingDouble(RadialChartData::getValue)).collect(Collectors.toList());
        List<RadialChartData> dataList       = tile.getRadialChartData();
        int                   noOfItems      = dataList.size();
        double                max            = dataList.stream().max(Comparator.comparingDouble(RadialChartData::getValue)).get().getValue();

        double                nameX          = radius * 0.975;
        double                nameWidth      = radius * 0.95;
        double                valueY         = radius * 0.94;
        double                valueWidth     = barWidth * 0.9;

        Color                 bkgColor       = Color.color(tile.getTextColor().getRed(), tile.getTextColor().getGreen(), tile.getTextColor().getBlue(), 0.15);

        ctx.clearRect(0, 0, canvasSize, canvasSize);
        ctx.setLineCap(StrokeLineCap.BUTT);
        ctx.setFill(tile.getTextColor());
        ctx.setTextAlign(TextAlignment.RIGHT);
        ctx.setTextBaseline(VPos.CENTER);
        ctx.setFont(Fonts.latoRegular(barWidth * 0.5));

        ctx.setStroke(bkgColor);
        ctx.setLineWidth(1);
        ctx.strokeLine(radius, 0, radius, radius - barWidth * 0.875);
        ctx.strokeLine(0, radius, radius - barWidth * 0.875, radius);
        ctx.strokeArc(noOfItems * barWidth, noOfItems * barWidth, canvasSize - (2 * noOfItems * barWidth), canvasSize - (2 * noOfItems * barWidth), 90, -270, ArcType.OPEN);

        for (int i = 0 ; i < noOfItems ; i++) {
            RadialChartData data  = dataList.get(i);
            double          value = data.getValue();
            double          bkgXY = i * barWidth;
            double          bkgWH = canvasSize - (2 * i * barWidth);
            double          barXY = barWidth * 0.5 + i * barWidth;
            double          barWH = canvasSize - barWidth - (2 * i * barWidth);
            double          angle = value / max * 270.0;

            // Background
            ctx.setLineWidth(1);
            ctx.setStroke(bkgColor);
            ctx.strokeArc(bkgXY, bkgXY, bkgWH, bkgWH, 90, -270, ArcType.OPEN);

            // DataBar
            ctx.setLineWidth(barWidth);
            ctx.setStroke(data.getColor());
            ctx.strokeArc(barXY, barXY, barWH, barWH, 90, -angle, ArcType.OPEN);

            // Name
            ctx.setTextAlign(TextAlignment.RIGHT);
            ctx.fillText(data.getName(), nameX, barXY, nameWidth);

            // Value
            ctx.setTextAlign(TextAlignment.CENTER);
            ctx.fillText(String.format(Locale.US, "%.0f", value), barXY, valueY, valueWidth);
        }
    }


    // ******************** Resizing ******************************************
    @Override protected void resizeStaticText() {
        double maxWidth = width - size * 0.1;
        double fontSize = size * textSize.factor;

        titleText.setFont(Fonts.latoRegular(fontSize));
        if (titleText.getLayoutBounds().getWidth() > maxWidth) { Helper.adjustTextSize(titleText, maxWidth, fontSize); }
        titleText.relocate(size * 0.05, size * 0.05);

        text.setFont(Fonts.latoRegular(fontSize));
        if (text.getLayoutBounds().getWidth() > maxWidth) { Helper.adjustTextSize(text, maxWidth, fontSize); }
        text.setX(size * 0.05);
        text.setY(height - size * 0.05);
    };

    @Override protected void resize() {
        width  = tile.getWidth() - tile.getInsets().getLeft() - tile.getInsets().getRight();
        height = tile.getHeight() - tile.getInsets().getTop() - tile.getInsets().getBottom();
        size   = width < height ? width : height;

        double canvasWidth  = width - size * 0.1;
        double canvasHeight = tile.isTextVisible() ? height - size * 0.28 : height - size * 0.205;
        double canvasSize   = canvasWidth < canvasHeight ? canvasWidth : canvasHeight;

        if (width > 0 && height > 0) {
            pane.setMaxSize(width, height);
            pane.setPrefSize(width, height);

            canvas.setWidth(canvasSize);
            canvas.setHeight(canvasSize);

            canvas.relocate((width - canvasSize) * 0.5, height * 0.15 + (height * (tile.isTextVisible() ? 0.75 : 0.85) - canvasSize) * 0.5);

            resizeStaticText();
        }
    };

    @Override protected void redraw() {
        super.redraw();
        titleText.setText(tile.getTitle());
        text.setText(tile.getText());

        resizeStaticText();
        drawChart();

        titleText.setFill(tile.getTitleColor());
        text.setFill(tile.getTextColor());
    };
}