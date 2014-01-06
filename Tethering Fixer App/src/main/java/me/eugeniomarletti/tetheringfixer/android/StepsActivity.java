/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Eugenio Marletti
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package me.eugeniomarletti.tetheringfixer.android;

import android.animation.Animator;
import android.app.ActionBar;
import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import me.eugeniomarletti.tetheringfixer.Async;
import me.eugeniomarletti.tetheringfixer.Fixer;
import me.eugeniomarletti.tetheringfixer.R;
import me.eugeniomarletti.tetheringfixer.Steps;
import me.eugeniomarletti.tetheringfixer.Utils;

import java.util.ArrayList;
import java.util.List;

import static me.eugeniomarletti.tetheringfixer.Utils.addOnGlobalLayoutListener;
import static me.eugeniomarletti.tetheringfixer.Utils.getOffsetBetweenViews;

public final class StepsActivity extends Activity implements Steps.StepListListener
{
    private static final int CLICK_RETRY = R.string.click_retry;

    private static final Interpolator ACCELERATE_INTERPOLATOR = new AccelerateInterpolator();
    private static final Interpolator DECELERATE_INTERPOLATOR = new DecelerateInterpolator();

    private static final int          CARD_SLIDE_DURATION     = 750;
    private static final Interpolator CARD_SLIDE_INTERPOLATOR = DECELERATE_INTERPOLATOR;

    private static final int          TEXT_FIXED_FADE_DURATION     = 500;
    private static final Interpolator TEXT_FIXED_FADE_INTERPOLATOR = DECELERATE_INTERPOLATOR;

    private static final int          TEXT_AROUND_FADE_IN_DURATION     = 400;
    private static final Interpolator TEXT_AROUND_FADE_IN_INTERPOLATOR = DECELERATE_INTERPOLATOR;

    private static final int          TEXT_AROUND_FADE_OUT_DURATION     = TEXT_AROUND_FADE_IN_DURATION;
    private static final Interpolator TEXT_AROUND_FADE_OUT_INTERPOLATOR = ACCELERATE_INTERPOLATOR;

    private List<ListItem> listItems;

    private FrameLayout        card;
    private RelativeLayout     list;
    private BulletExpandEffect bulletExpandEffect;
    private TextView           textClick;
    private TextView           textSuccess;
    private TextView           textError;
    private CompoundButton     fixAtBoot;

    private float    cardCenteredY;
    private int      bulletColorChecked;
    private Drawable background;

    private void adjustSwitchMargin(boolean afterLayout)
    {
        if (fixAtBoot == null) return;
        final Runnable adjustSwitchMargin = new Runnable()
        {
            @Override
            public void run()
            {
                final int margin = (((View)fixAtBoot.getParent()).getHeight() - fixAtBoot.getHeight()) / 2;
                if (fixAtBoot.getRight() != margin)
                {
                    final ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams)fixAtBoot.getLayoutParams();
                    lp.rightMargin = margin;
                    fixAtBoot.setLayoutParams(lp);
                }
            }
        };
        if (!afterLayout) adjustSwitchMargin.run();
        else addOnGlobalLayoutListener(fixAtBoot, true, new ViewTreeObserver.OnGlobalLayoutListener()
        {
            @Override
            public void onGlobalLayout()
            {
                adjustSwitchMargin.run();
            }
        });
    }

    private void fadeTextAround(final View view, final boolean in)
    {
        if (view.getVisibility() == View.VISIBLE)
        {
            if (in) view.setAlpha(0f);
            view.animate()
                .withLayer()
                .alpha(in ? 1f : 0f)
                .setDuration(in ? TEXT_AROUND_FADE_IN_DURATION : TEXT_AROUND_FADE_OUT_DURATION)
                .setInterpolator(in ? TEXT_AROUND_FADE_IN_INTERPOLATOR : TEXT_AROUND_FADE_OUT_INTERPOLATOR);
        }
    }

    private void setCurrentListItem(int index, boolean isError, final String errorText, boolean animate)
    {
        int i = 0;
        for (ListItem listItem : listItems)
        {
            boolean activated, checked, working, error;

            if (i < index)
            {
                // before current
                checked = true;
                activated = false;
                working = false;
                error = false;
            }
            else if (i == index)
            {
                // current
                checked = false;
                activated = true;
                working = !isError;
                error = isError;
            }
            else
            {
                // after current
                checked = false;
                activated = false;
                working = false;
                error = false;
            }

            listItem.setError(error);
            listItem.setActivated(activated);
            listItem.setChecked(checked);
            listItem.setWorking(working);

            i++;
        }

        if (isError)
        {
            card.setClickable(true);

            textClick.setText(CLICK_RETRY);
            textClick.setVisibility(View.VISIBLE);
            if (errorText != null)
            {
                textError.setText(Utils.addEmoji(errorText));
                textError.setVisibility(View.VISIBLE);
            }
            else
            {
                textError.setVisibility(View.GONE);
                textError.setText(null);
            }
            if (animate)
            {
                final int oldCardPosition = Utils.getLocationInWindow(card).y;
                addOnGlobalLayoutListener(textClick, true, new ViewTreeObserver.OnGlobalLayoutListener()
                {
                    @Override
                    public void onGlobalLayout()
                    {
                        fadeTextAround(textClick, true);
                        if (errorText != null) fadeTextAround(textError, true);
                        card.setTranslationY(oldCardPosition - Utils.getLocationInWindow(card).y);
                        card.animate()
                            .translationY(0f)
                            .setDuration(CARD_SLIDE_DURATION)
                            .setInterpolator(CARD_SLIDE_INTERPOLATOR)
                            .setListener(null);
                    }
                });
            }
        }
    }

    private void setCurrentListItem(int index)
    {
        setCurrentListItem(index, false, null, true);
    }

    private void success(int index)
    {
        setCurrentListItem(index);
        final ListItem currentListItem = listItems.get(index);
        final Bullet bullet = currentListItem.getBullet();
        bullet.addBulletColorAnimationListener(new SimpleAnimatorListener()
        {
            @Override
            public void onAnimationEnd(Animator animator)
            {
                bullet.removeBulletColorAnimationListener(this);

                final Point offset = getOffsetBetweenViews(bulletExpandEffect, bullet);
                final PointF bulletCenter = bullet.getBulletCenter();
                bulletCenter.offset(offset.x, offset.y);

                bulletExpandEffect.setVisibility(View.VISIBLE); // needs to do layout to be effective
                addOnGlobalLayoutListener(bulletExpandEffect, true, new ViewTreeObserver
                        .OnGlobalLayoutListener()
                {
                    @Override
                    public void onGlobalLayout()
                    {
                        bulletExpandEffect.animateExpand(
                                bulletColorChecked, bulletCenter, bullet.getBulletSize(), new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                // expanded
                                success(true);
                            }
                        });
                    }
                });
            }
        });
        currentListItem.setWorking(false);
        currentListItem.setChecked(true);
    }

    private void success(boolean animate)
    {
        for (ListItem listItem : listItems) listItem.setVisibility(View.INVISIBLE);
        ((View)bulletExpandEffect.getParent()).setBackgroundColor(bulletColorChecked);
        bulletExpandEffect.setVisibility(View.GONE);
        textSuccess.setVisibility(View.VISIBLE);
        if (animate)
        {
            textSuccess.setAlpha(0f);
            textSuccess.animate()
                       .alpha(1f)
                       .setDuration(TEXT_FIXED_FADE_DURATION)
                       .setInterpolator(TEXT_FIXED_FADE_INTERPOLATOR)
                       .withLayer();
        }
        else textSuccess.setAlpha(1f);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        adjustSwitchMargin(true);
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fixer);

        list = (RelativeLayout)findViewById(R.id.list);
        textSuccess = (TextView)findViewById(R.id.text_success);
        textError = (TextView)findViewById(R.id.text_error);
        textClick = (TextView)findViewById(R.id.text_click);
        card = (FrameLayout)findViewById(R.id.card);
        bulletExpandEffect = (BulletExpandEffect)findViewById(R.id.bullet_expand_effect);

        background = ((View)bulletExpandEffect.getParent()).getBackground();

        textSuccess.setText(Utils.addEmoji(textSuccess.getText()));

        card.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                Steps.startOrRetry();
            }
        });

        Fixer.isFixAtBootEnabledAsync(new Async.SimpleMainThreadCallback<Boolean>()
        {
            @Override
            public void mainThreadCallback(Boolean result, boolean success, Throwable error)
            {
                if (!success || result == null) throw new RuntimeException(error);
                final ActionBar ab = getActionBar();
                ab.setCustomView(R.layout.activity_fixer_actionbar);
                fixAtBoot = (CompoundButton)ab.getCustomView().findViewById(R.id.fix_at_boot_switch);
                fixAtBoot.setChecked(result);
                fixAtBoot.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
                {
                    @Override
                    public void onCheckedChanged(final CompoundButton button, boolean enabled)
                    {
                        button.setEnabled(false);
                        Fixer.setFixAtBootEnabledAsync(new Async.SimpleMainThreadCallback<Void>()
                        {
                            @Override
                            public void mainThreadCallback(Void result, boolean success, Throwable error)
                            {
                                if (!success) throw new RuntimeException(error); // can do better when i'm not lazy
                                if (Steps.isSuccess()) Fixer.shutdown();
                                button.setEnabled(true);
                            }
                        }, enabled);
                    }
                });
                ab.setDisplayShowCustomEnabled(true);
                adjustSwitchMargin(true);
            }
        });

        addOnGlobalLayoutListener(card, true, new ViewTreeObserver.OnGlobalLayoutListener()
        {
            @Override
            public void onGlobalLayout()
            {
                cardCenteredY = Utils.getLocationInWindow(card).y;
            }
        });

        addOnGlobalLayoutListener(bulletExpandEffect, true, new ViewTreeObserver.OnGlobalLayoutListener()
        {
            @Override
            public void onGlobalLayout()
            {
                final View parent = (View)bulletExpandEffect.getParent();
                final FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams)bulletExpandEffect.getLayoutParams();
                lp.width = parent.getWidth() - parent.getPaddingLeft() - parent.getPaddingRight();
                lp.height = parent.getHeight() - parent.getPaddingTop() - parent.getPaddingBottom();
                bulletExpandEffect.setLayoutParams(lp);
            }
        });

        // inflate views for list items
        final List<String> labels = Steps.getLabels();
        listItems = new ArrayList<>(labels.size());
        for (int i = 0, labelsLength = labels.size(); i < labelsLength; i++)
        {
            final ListItem previousListItem = i > 0 ? listItems.get(i - 1) : null;
            final LayoutInflater inflater = getLayoutInflater();
            final Bullet bullet = (Bullet)inflater.inflate(R.layout.activity_fixer_bullet, list, false);
            final TextView label = (TextView)inflater.inflate(R.layout.activity_fixer_label, list, false);
            final int bulletId = Utils.generateViewId();
            final int labelId = Utils.generateViewId();
            bullet.setId(bulletId);
            label.setId(labelId);
            label.setText(labels.get(i));
            RelativeLayout.LayoutParams bulletLayoutParams = (RelativeLayout.LayoutParams)bullet.getLayoutParams();
            RelativeLayout.LayoutParams labelLayoutParams = (RelativeLayout.LayoutParams)label.getLayoutParams();
            if (previousListItem != null)
                bulletLayoutParams.addRule(RelativeLayout.BELOW, previousListItem.getBullet().getId());
            labelLayoutParams.addRule(RelativeLayout.RIGHT_OF, bulletId);
            labelLayoutParams.addRule(RelativeLayout.ALIGN_TOP, bulletId);
            labelLayoutParams.addRule(RelativeLayout.ALIGN_BOTTOM, bulletId);
            list.addView(bullet, bulletLayoutParams);
            list.addView(label, labelLayoutParams);
            listItems.add(new ListItem(bullet, label));
        }
        bulletColorChecked = listItems.get(0).getBullet().getBulletColorChecked();
    }

    @Override
    protected void onStart()
    {
        super.onStart();

        addOnGlobalLayoutListener(card, true, new ViewTreeObserver.OnGlobalLayoutListener()
        {
            @Override
            public void onGlobalLayout()
            {
                Steps.addListener(StepsActivity.this);
                final boolean isStarted = Steps.isStarted();
                final boolean isError = Steps.isError();
                final boolean isSuccess = Steps.isSuccess();
                textClick.setVisibility(!isStarted || !isSuccess ? View.VISIBLE : View.GONE);
                card.setClickable(!isStarted || isError);
                if (isSuccess) success(false);
                else
                {
                    for (ListItem listItem : listItems) listItem.setVisibility(View.VISIBLE);
                    ((View)bulletExpandEffect.getParent()).setBackground(background);
                    bulletExpandEffect.setVisibility(View.GONE);
                    textSuccess.setVisibility(View.GONE);
                    setCurrentListItem(Steps.getCurrentStep(), isError, Steps.getErrorText(), false);
                }
            }
        });
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        StepsService.cancelNotification();
        StepsService.stop();

        Steps.setActionsDelayed(true);
        Steps.resume();
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        Steps.pause();
        Steps.setActionsDelayed(false);
    }

    @Override
    protected void onStop()
    {
        super.onStop();

        Steps.removeListener(this);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        Steps.removeListener(this);
        Steps.shutdownIfNoListeners();
    }

    @Override
    public void onStepsStart()
    {
        Utils.runOnMainThread(new Runnable()
        {
            @Override
            public void run()
            {
                card.setClickable(false);
                fadeTextAround(textClick, false);
                fadeTextAround(textError, false);
                card.animate()
                    .translationYBy(cardCenteredY - Utils.getLocationInWindow(card).y)
                    .setDuration(CARD_SLIDE_DURATION)
                    .setInterpolator(CARD_SLIDE_INTERPOLATOR)
                    .setListener(new SimpleAnimatorListener()
                    {
                        @Override
                        public void onAnimationEnd(Animator animator)
                        {
                            card.animate().setListener(null);
                            textClick.setVisibility(View.GONE);
                            textError.setVisibility(View.GONE);
                            card.setTranslationY(0f);
                        }
                    });
            }
        });
    }

    @Override
    public void onStepsRetry()
    {
        setCurrentListItem(-1);
    }

    @Override
    public void onStepError(final int itemIndex, final String errorText)
    {
        setCurrentListItem(itemIndex, true, errorText, true);
    }

    @Override
    public void onAdvanceStep(final int itemIndex)
    {
        setCurrentListItem(itemIndex);
    }

    @Override
    public void onStepsSuccess(final int itemIndex)
    {
        success(itemIndex);
    }

    private static final class ListItem
    {
        private final Bullet   bullet;
        private final TextView label;

        public ListItem(Bullet bullet, TextView label)
        {
            this.bullet = bullet;
            this.label = label;
        }

        public Bullet getBullet()
        {
            return bullet;
        }

        public TextView getLabel()
        {
            return label;
        }

        public void setError(boolean error)
        {
            bullet.setError(error);
            label.setError(error);
        }

        public void setWorking(boolean working)
        {
            bullet.setWorking(working);
            label.setWorking(working);
        }

        public void setActivated(boolean activated)
        {
            bullet.setActivated(activated);
            label.setActivated(activated);
        }

        public void setChecked(boolean checked)
        {
            bullet.setChecked(checked);
            label.setChecked(checked);
        }

        public void setVisibility(int visibility)
        {
            bullet.setVisibility(visibility);
            label.setVisibility(visibility);
        }
    }
}
