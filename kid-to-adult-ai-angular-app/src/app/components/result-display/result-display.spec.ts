import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ResultDisplay } from './result-display';

describe('ResultDisplay', () => {
  let component: ResultDisplay;
  let fixture: ComponentFixture<ResultDisplay>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ResultDisplay]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ResultDisplay);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
