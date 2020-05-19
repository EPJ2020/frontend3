package com.example.lfg_source.main;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;

import com.example.lfg_source.R;
import com.example.lfg_source.entity.Group;
import com.example.lfg_source.entity.User;
import com.example.lfg_source.login.Login;
import com.example.lfg_source.main.edit.UserEditFragment;
import com.example.lfg_source.main.home.HomeFragment;
import com.example.lfg_source.main.match.MatchFragment;
import com.example.lfg_source.main.match.MatchUserFragment;
import com.example.lfg_source.main.swipe.GroupSwipeFragment;
import com.example.lfg_source.main.swipe.SwipeFragment;
import com.example.lfg_source.main.swipe.UserSwipeFragment;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

  private Toolbar toolbar;
  private Spinner spinner;
  private Button helpButton;
  private MainFacade facade;
  private ShowcaseView showCase;

  private static final int requestCode = 1;
  private HomeFragment homeFragment = null;
  private Fragment selectedFragment;
  private String token;
  private User loggedInUser;
  private Boolean isMatchFragment = false;
  private List<Object> spinnerList = new ArrayList<>();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    toolbar = findViewById(R.id.toolbar);
    spinner = findViewById(R.id.spinner);
    helpButton = findViewById(R.id.help_button);
    setupSpinnerListener();

    SharedPreferences preferences = this.getSharedPreferences(getResources().getString(R.string.app_name), Context.MODE_PRIVATE);
    token = preferences.getString(getResources().getString(R.string.usertoken), null);

    if (token == null) {
      Intent i = new Intent(getApplicationContext(), Login.class);
      startActivityForResult(i, requestCode);
    } else {
      setup();
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == this.requestCode) {
      if (resultCode == Activity.RESULT_OK) {
        token = data.getStringExtra(getResources().getString(R.string.usertoken));
        SharedPreferences preferences = this.getSharedPreferences("LFG", Context.MODE_PRIVATE);
        preferences.edit().putString(getResources().getString(R.string.usertoken), token).apply();
        setup();
      }
    }
  }

  public void setUser(User user) {
    loggedInUser = user;
    finishSetup();
  }

  private void setup() {
    facade = new MainFacade(this);
    facade
        .getService()
        .getLoginUser()
        .observe(
            this,
            new Observer<User>() {
              @Override
              public void onChanged(User user) {
                loggedInUser = user;
                finishSetup();
              }
            });
    facade
        .getService()
        .getMyGroups()
        .observe(
            this,
            new Observer<List<Group>>() {
              @Override
              public void onChanged(List<Group> myGroups) {
                spinnerList.clear();
                spinnerList.addAll(myGroups);
                setupToolbar2();
              }
            });
    facade.getMyProfile();
  }

  private void finishSetup() {
    FragmentManager fragmentManager = getSupportFragmentManager();
    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
    helpButton.setOnClickListener(
        new View.OnClickListener() {
          public void onClick(View v) {
            help1("Da können sie Ihre Rolle auswählen", MainActivity.this, R.id.spinner);
          }
        });
    if (loggedInUser == null) {
      setNullToolbar("Profil anlegen");
      UserEditFragment userEditFragment = new UserEditFragment();
      fragmentTransaction.add(R.id.fragment_container, userEditFragment);
      fragmentTransaction.commit();
    } else {
      homeFragment = new HomeFragment(loggedInUser);
      fragmentTransaction.add(R.id.fragment_container, homeFragment);
      fragmentTransaction.commit();

      BottomNavigationView bottomNavigationView = findViewById(R.id.mainNavigation);
      bottomNavigationView.setOnNavigationItemSelectedListener(
          new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
              switch (item.getItemId()) {
                case R.id.action_swipe:
                  facade.getMyGroups();
                  isMatchFragment = false;
                  selectedFragment = new GroupSwipeFragment(loggedInUser);
                  break;
                case R.id.action_Matches:
                  facade.getMyGroups();
                  isMatchFragment = true;
                  selectedFragment = new MatchFragment();
                  break;
                default:
                  selectedFragment = new HomeFragment(loggedInUser);
                  isMatchFragment = false;
                  break;
              }
              getSupportFragmentManager()
                  .beginTransaction()
                  .replace(R.id.fragment_container, selectedFragment)
                  .commit();
              return true;
            }
          });
    }
  }

  private void help1(String text, Activity activity, int item) {
    // Code Functionality ShowcaseView from https://github.com/amlcurran/ShowcaseView
    showCase =
        new ShowcaseView.Builder(MainActivity.this)
            .setStyle(R.style.CustomShowcaseTheme2)
            .setTarget(new ViewTarget(item, activity))
            .setContentTitle("Dropdown")
            .setContentText(text)
            .setOnClickListener(
                new View.OnClickListener() {
                  public void onClick(View v) {
                    Fragment fragment =
                        MainActivity.this
                            .getSupportFragmentManager()
                            .findFragmentById(R.id.fragment_container);
                    if (fragment instanceof SwipeFragment) {
                      help2(
                          "Hier werden für die Oben ausgewählte Rolle Vorschläge gezeigt, die Sie mit rechts-Swipe annehmen und mit links-Swipe ablehnen.",
                          fragment.getActivity(),
                          R.id.circularProgressbar);

                    } else {
                      help2(
                          "Hier finden sie die Kontaktdaten zu den Vorschlägen, die von beiden Seiten angenommen wurden.",
                          fragment.getActivity(),
                          R.id.yourMatchesList);
                    }
                  }
                })
            .hideOnTouchOutside()
            .withHoloShowcase()
            .build();
  }

  private void help2(String text, Activity activity, int item) {
    // Code Functionality ShowcaseView from https://github.com/amlcurran/ShowcaseView
    showCase.hide();
    new ShowcaseView.Builder(MainActivity.this)
        .setStyle(R.style.CustomShowcaseTheme2)
        .setTarget(new ViewTarget(item, activity))
        .setContentTitle("Was ist das?")
        .setContentText(text)
        .hideOnTouchOutside()
        .withHoloShowcase()
        .build();
  }

  private void setupSpinnerListener() {
    spinner.setOnItemSelectedListener(
        new AdapterView.OnItemSelectedListener() {
          public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            Object item = parent.getItemAtPosition(pos);
            if (item instanceof Group && isMatchFragment) {
              final Group group = (Group) item;
              selectedFragment = new MatchUserFragment(group);
            }
            if (item instanceof User && isMatchFragment) {
              selectedFragment = new MatchFragment();
            }
            if (item instanceof Group && !isMatchFragment) {
              final Group group = (Group) item;
              selectedFragment = new UserSwipeFragment(group);
            }
            if (item instanceof User && !isMatchFragment) {
              selectedFragment = new GroupSwipeFragment(loggedInUser);
            }
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

            fragmentTransaction.replace(R.id.fragment_container, selectedFragment);
            fragmentTransaction.commit();
          }

          @Override
          public void onNothingSelected(AdapterView<?> parent) {}
        });
  }

  public void setupToolbar2() {
    spinnerList.add(loggedInUser);
    ArrayAdapter<Object> adapter =
        new ArrayAdapter<Object>(getApplicationContext(), R.layout.spin_item, spinnerList);
    adapter.setDropDownViewResource(R.layout.spin_dropdown_item);
    spinner.setVisibility(View.VISIBLE);
    helpButton.setVisibility(View.VISIBLE);
    spinner.setAdapter(adapter);
    Fragment fragment = this.getSupportFragmentManager().findFragmentById(R.id.fragment_container);
    if (fragment instanceof HomeFragment) {
      spinner.setSelection(((HomeFragment) fragment).getSelectedGroupPosition());
    } else {
      spinner.setSelection(spinnerList.size() - 1);
    }
  }

  public void setNullToolbar(String title) {
    spinner.setVisibility(View.GONE);
    helpButton.setVisibility(View.GONE);
    toolbar.setTitle(title);
    isMatchFragment = false;
  }

  public MainFacade getMainFacade() {
    return facade;
  }

  @Override
  public void onBackPressed() {
    moveTaskToBack(false);
  }
}
