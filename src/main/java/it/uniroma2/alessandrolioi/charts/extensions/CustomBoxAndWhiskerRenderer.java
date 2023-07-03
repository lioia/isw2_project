package it.uniroma2.alessandrolioi.charts.extensions;

import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.chart.renderer.category.CategoryItemRendererState;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.statistics.BoxAndWhiskerCategoryDataset;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

public class CustomBoxAndWhiskerRenderer extends BoxAndWhiskerRenderer {
    public CustomBoxAndWhiskerRenderer() {
        super();

        this.setMeanVisible(false);
    }

    @Override
    public void drawVerticalItem(Graphics2D g2, CategoryItemRendererState state, Rectangle2D dataArea,
                                 CategoryPlot plot, CategoryAxis domainAxis, ValueAxis rangeAxis,
                                 CategoryDataset dataset, int row, int column) {
        BoxAndWhiskerCategoryDataset bawDataset = (BoxAndWhiskerCategoryDataset) dataset;

        double categoryEnd = domainAxis.getCategoryEnd(column, getColumnCount(), dataArea, plot.getDomainAxisEdge());
        double categoryStart = domainAxis.getCategoryStart(column, getColumnCount(), dataArea, plot.getDomainAxisEdge());
        double categoryWidth = categoryEnd - categoryStart;

        double xx = categoryStart;
        int seriesCount = getRowCount();
        int categoryCount = getColumnCount();

        if (seriesCount > 1) {
            double seriesGap = dataArea.getWidth() * getItemMargin() / (categoryCount * (seriesCount - 1));
            double usedWidth = (state.getBarWidth() * seriesCount) + (seriesGap * (seriesCount - 1));
            // offset the start of the boxes if the total width used is smaller
            // than the category width
            double offset = (categoryWidth - usedWidth) / 2;
            xx = xx + offset + (row * (state.getBarWidth() + seriesGap));
        } else {
            // offset the start of the box if the box width is smaller than the
            // category width
            double offset = (categoryWidth - state.getBarWidth()) / 2;
            xx = xx + offset;
        }

        Paint itemPaint = getItemPaint(row, column);
        g2.setPaint(itemPaint);
        Stroke s = getItemStroke(row, column);
        g2.setStroke(s);

        RectangleEdge location = plot.getRangeAxisEdge();

        Number yQ1 = bawDataset.getQ1Value(row, column);
        Number yQ3 = bawDataset.getQ3Value(row, column);
        Number yMax = bawDataset.getMaxRegularValue(row, column);
        Number yMin = bawDataset.getMinRegularValue(row, column);
        Shape box = null;
        if (yQ1 != null && yQ3 != null && yMax != null && yMin != null) {
            double yyQ1 = rangeAxis.valueToJava2D(yQ1.doubleValue(), dataArea, location);
            double yyQ3 = rangeAxis.valueToJava2D(yQ3.doubleValue(), dataArea, location);
            double yyMax = rangeAxis.valueToJava2D(yMax.doubleValue(), dataArea, location);
            double yyMin = rangeAxis.valueToJava2D(yMin.doubleValue(), dataArea, location);
            double xxMid = xx + state.getBarWidth() / 2.0;
            double halfW = (state.getBarWidth() / 2.0) * getWhiskerWidth();

            // draw the body...
            box = new Rectangle2D.Double(xx, Math.min(yyQ1, yyQ3), state.getBarWidth(), Math.abs(yyQ1 - yyQ3));
            if (getFillBox())
                g2.fill(box);

            Paint outlinePaint = getItemOutlinePaint(row, column);
            if (getUseOutlinePaintForWhiskers())
                g2.setPaint(outlinePaint);

            // draw the upper shadow...
            g2.draw(new Line2D.Double(xxMid, yyMax, xxMid, yyQ3));
            g2.draw(new Line2D.Double(xxMid - halfW, yyMax, xxMid + halfW, yyMax));

            // draw the lower shadow...
            g2.draw(new Line2D.Double(xxMid, yyMin, xxMid, yyQ1));
            g2.draw(new Line2D.Double(xxMid - halfW, yyMin, xxMid + halfW, yyMin));

            g2.setStroke(getItemOutlineStroke(row, column));
            g2.setPaint(outlinePaint);
            g2.draw(box);
        }

        g2.setPaint(getArtifactPaint());

        // draw median...
        if (isMedianVisible()) {
            Number yMedian = bawDataset.getMedianValue(row, column);
            if (yMedian != null) {
                double yyMedian = rangeAxis.valueToJava2D(yMedian.doubleValue(), dataArea, location);
                g2.draw(new Line2D.Double(xx, yyMedian, xx + state.getBarWidth(), yyMedian));
            }
        }

        g2.setPaint(itemPaint);

        // collect entity and tool tip information...
        if (state.getInfo() != null && box != null) {
            EntityCollection entities = state.getEntityCollection();
            if (entities != null)
                addItemEntity(entities, dataset, row, column, box);
        }
    }
}
